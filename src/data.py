import json
from datetime import date
from pathlib import Path


def parse_date(value):
    if not isinstance(value, str):
        raise ValueError("дата должна быть строкой YYYY-MM-DD")
    parsed = date.fromisoformat(value)
    if parsed.isoformat() != value:
        raise ValueError("дата должна иметь формат YYYY-MM-DD")
    return parsed


def get_record(records, record_id, entity):
    for record in records:
        if record["id"] == record_id:
            return record
    raise ValueError(f"{entity}: запись {record_id!r} не найдена")


def validate_data(data):
    if not isinstance(data, dict):
        raise ValueError("корень JSON должен быть объектом")
    tables = (
        "clients", "orders", "employees", "order_members", "stages", "tasks",
        "materials", "material_versions", "task_materials", "approvals", "comments",
    )
    indexes = {}
    for table in tables:
        rows = data.get(table)
        if not isinstance(rows, list) or any(not isinstance(row, dict) for row in rows):
            raise ValueError(f"{table}: ожидается список записей")
        if table in ("order_members", "task_materials"):
            continue
        indexes[table] = {}
        for row in rows:
            record_id = row.get("id")
            if not isinstance(record_id, str) or not record_id.strip():
                raise ValueError(f"{table}: нужен непустой строковый id")
            if record_id in indexes[table]:
                raise ValueError(f"{table}: повторяющийся id {record_id!r}")
            indexes[table][record_id] = row

    text_fields = (
        ("clients", ("name", "contact")),
        ("orders", ("title",)),
        ("employees", ("name", "specialty")),
        ("order_members", ("project_role",)),
        ("stages", ("name",)),
        ("tasks", ("title",)),
        ("materials", ("name",)),
        ("material_versions", ("url",)),
        ("comments", ("text",)),
    )
    for table, fields in text_fields:
        for row in data[table]:
            for field in fields:
                if not isinstance(row.get(field), str) or not row[field].strip():
                    raise ValueError(f"{table}: поле {field} должно быть заполнено")

    references = (
        ("orders", "client_id", "clients"),
        ("order_members", "order_id", "orders"),
        ("order_members", "employee_id", "employees"),
        ("stages", "order_id", "orders"),
        ("tasks", "stage_id", "stages"),
        ("tasks", "employee_id", "employees"),
        ("materials", "stage_id", "stages"),
        ("material_versions", "material_id", "materials"),
        ("task_materials", "task_id", "tasks"),
        ("task_materials", "material_id", "materials"),
        ("approvals", "version_id", "material_versions"),
        ("comments", "version_id", "material_versions"),
    )
    for table, field, target in references:
        for row in data[table]:
            value = row.get(field)
            if not isinstance(value, str) or value not in indexes[target]:
                raise ValueError(f"{table}: неверная ссылка {field}={value!r}")

    states = (
        ("orders", "status", ("new", "in_progress", "completed", "cancelled")),
        ("stages", "status", ("planned", "in_progress", "review", "approved", "completed")),
        ("tasks", "status", ("todo", "in_progress", "done", "cancelled")),
        ("material_versions", "review_status", ("pending", "passed", "failed")),
        ("approvals", "decision", ("approved", "changes_requested", "rejected")),
        ("comments", "status", ("open", "resolved")),
    )
    for table, field, allowed in states:
        for row in data[table]:
            if row.get(field) not in allowed:
                raise ValueError(f"{table}: неизвестное значение {field}={row.get(field)!r}")

    memberships = set()
    for row in data["order_members"]:
        key = (row["order_id"], row["employee_id"])
        if key in memberships:
            raise ValueError("order_members: повторное участие сотрудника в заказе")
        memberships.add(key)
    for task in data["tasks"]:
        if type(task.get("required")) is not bool:
            raise ValueError("tasks: required должен быть логическим значением")
        parse_date(task.get("deadline"))
        order_id = indexes["stages"][task["stage_id"]]["order_id"]
        if (order_id, task["employee_id"]) not in memberships:
            raise ValueError(f"tasks: исполнитель {task['id']} не входит в команду заказа")
        comment_id = task.get("comment_id")
        if comment_id is not None:
            if not isinstance(comment_id, str) or comment_id not in indexes["comments"]:
                raise ValueError("tasks: неверная ссылка comment_id")
            comment = indexes["comments"][comment_id]
            version = indexes["material_versions"][comment["version_id"]]
            material = indexes["materials"][version["material_id"]]
            if material["stage_id"] != task["stage_id"]:
                raise ValueError("tasks: замечание относится к другому этапу")

    links = set()
    for row in data["task_materials"]:
        key = (row["task_id"], row["material_id"])
        if key in links:
            raise ValueError("task_materials: повторная связь задачи и материала")
        links.add(key)
        if indexes["tasks"][row["task_id"]]["stage_id"] != indexes["materials"][row["material_id"]]["stage_id"]:
            raise ValueError("task_materials: задача и материал относятся к разным этапам")

    version_numbers = set()
    for row in data["material_versions"]:
        number = row.get("number")
        if type(number) is not int or number < 1:
            raise ValueError("material_versions: number должен быть положительным целым")
        key = (row["material_id"], number)
        if key in version_numbers:
            raise ValueError("material_versions: повторный номер версии материала")
        version_numbers.add(key)
    for approval in data["approvals"]:
        parse_date(approval.get("decided_on"))


def load_data(filename):
    with Path(filename).open(encoding="utf-8") as source:
        data = json.load(source)
    validate_data(data)
    return data
