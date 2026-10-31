import math
from bisect import bisect_left

from data import get_record, parse_date


def check_brief(order):
    errors = []
    fields = (
        ("goal", "цель кампании"),
        ("audience", "целевая аудитория"),
        ("format", "формат кампании"),
        ("contact", "контакт клиента"),
    )
    for field, label in fields:
        if not isinstance(order.get(field), str) or not order[field].strip():
            errors.append(f"Не заполнено поле: {label}.")
    budget = order.get("budget")
    if type(budget) not in (int, float) or budget <= 0 or (type(budget) is float and not math.isfinite(budget)):
        errors.append("Бюджет должен быть положительным конечным числом.")
    try:
        parse_date(order.get("deadline"))
    except ValueError:
        errors.append("Срок сдачи должен быть корректной датой YYYY-MM-DD.")
    return {"order_id": order["id"], "complete": not errors, "errors": errors}


def check_stage_readiness(data, stage_id):
    stage = get_record(data["stages"], stage_id, "Этап")
    order = get_record(data["orders"], stage["order_id"], "Заказ")
    blockers = []
    if order["status"] in ("completed", "cancelled"):
        blockers.append("Заказ закрыт: передача этапа на согласование недоступна.")
    if stage["status"] in ("approved", "completed"):
        blockers.append("Этап уже согласован или завершён.")
    tasks = [task for task in data["tasks"] if task["stage_id"] == stage_id and task["required"]]
    if not tasks:
        blockers.append("В этапе нет обязательных задач для проверки.")
    checked = []
    versions = []
    for task in tasks:
        if task["status"] != "done":
            blockers.append(f"Задача {task['id']} не завершена: {task['title']}.")
        for link in data["task_materials"]:
            if link["task_id"] != task["id"] or link["material_id"] in checked:
                continue
            material_id = link["material_id"]
            checked.append(material_id)
            version = None
            for candidate in data["material_versions"]:
                if candidate["material_id"] == material_id:
                    if version is None or candidate["number"] > version["number"]:
                        version = candidate
            if version is None:
                blockers.append(f"У материала {material_id} нет ни одной версии.")
                continue
            versions.append({"material_id": material_id, "version_id": version["id"]})
            if version["review_status"] != "passed":
                blockers.append(f"Последняя версия {version['id']} материала {material_id} не прошла внутреннюю проверку.")
            open_ids = [comment["id"] for comment in data["comments"] if comment["version_id"] == version["id"] and comment["status"] == "open"]
            if open_ids:
                ids = ", ".join(open_ids)
                blockers.append(f"У версии {version['id']} есть открытые замечания: {ids}.")
    if tasks and not checked:
        blockers.append("Для обязательных задач не указаны материалы результата.")
    return {
        "stage_id": stage_id, "order_id": stage["order_id"], "ready": not blockers,
        "required_tasks": len(tasks), "checked_materials": len(checked),
        "versions": versions, "blockers": blockers,
    }


def find_overdue_tasks(data, as_of, order_id=None):
    today = parse_date(as_of)
    if order_id is not None:
        get_record(data["orders"], order_id, "Заказ")
    active_orders = sorted(
        order["id"] for order in data["orders"]
        if order["status"] not in ("completed", "cancelled")
        and (order_id is None or order["id"] == order_id)
    )
    stages = []
    for stage in data["stages"]:
        position = bisect_left(active_orders, stage["order_id"])
        if position < len(active_orders) and active_orders[position] == stage["order_id"]:
            stages.append((stage["id"], stage["order_id"]))
    stages.sort()
    stage_ids = [stage[0] for stage in stages]
    overdue = []
    for task in data["tasks"]:
        position = bisect_left(stage_ids, task["stage_id"])
        if position == len(stage_ids) or stage_ids[position] != task["stage_id"] or task["status"] in ("done", "cancelled"):
            continue
        deadline = parse_date(task["deadline"])
        if deadline < today:
            overdue.append({
                "task_id": task["id"], "title": task["title"],
                "order_id": stages[position][1], "stage_id": task["stage_id"],
                "employee_id": task["employee_id"], "deadline": task["deadline"],
                "days_overdue": (today - deadline).days,
            })
    overdue.sort(key=lambda row: (row["deadline"], row["task_id"]))
    return {"as_of": as_of, "order_id": order_id, "tasks": overdue}


def build_order_summary(data, order_id, as_of):
    order = get_record(data["orders"], order_id, "Заказ")
    client = get_record(data["clients"], order["client_id"], "Клиент")
    stages = [stage for stage in data["stages"] if stage["order_id"] == order_id]
    stage_ids = {stage["id"] for stage in stages}
    tasks = [task for task in data["tasks"] if task["stage_id"] in stage_ids]
    materials = [material for material in data["materials"] if material["stage_id"] in stage_ids]
    material_ids = {material["id"] for material in materials}
    versions = [version for version in data["material_versions"] if version["material_id"] in material_ids]
    version_ids = {version["id"] for version in versions}
    approvals = [row for row in data["approvals"] if row["version_id"] in version_ids]
    comments = [row for row in data["comments"] if row["version_id"] in version_ids]
    employees = {employee["id"]: employee for employee in data["employees"]}
    team = []
    for membership in data["order_members"]:
        if membership["order_id"] == order_id:
            employee = employees[membership["employee_id"]]
            team.append({"employee_id": employee["id"], "name": employee["name"], "project_role": membership["project_role"]})
    overdue = find_overdue_tasks(data, as_of, order_id)["tasks"]
    return {
        "as_of": as_of, "order": dict(order), "client": dict(client), "team": team,
        "stages": [dict(row) for row in stages], "tasks": [dict(row) for row in tasks],
        "materials": [dict(row) for row in materials], "versions": [dict(row) for row in versions],
        "approvals": [dict(row) for row in approvals], "comments": [dict(row) for row in comments],
        "overdue_tasks": overdue,
        "totals": {
            "stages": len(stages), "tasks": len(tasks),
            "done_tasks": sum(task["status"] == "done" for task in tasks),
            "materials": len(materials), "versions": len(versions), "approvals": len(approvals),
            "open_comments": sum(comment["status"] == "open" for comment in comments),
            "overdue_tasks": len(overdue),
        },
    }
