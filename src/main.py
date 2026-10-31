import argparse
import json
import sys
from datetime import date
from pathlib import Path

from data import get_record, load_data
from operations import build_order_summary, check_brief, check_stage_readiness, find_overdue_tasks


def make_parser():
    parser = argparse.ArgumentParser(description="Учёт заказов агентства иммерсивного маркетинга")
    parser.add_argument("--data", type=Path, default=Path(__file__).with_name("sample_data.json"))
    parser.add_argument("--json", action="store_true", help="результат в формате JSON")
    parser.set_defaults(command="demo", order="O1", as_of=date.today().isoformat())
    commands = parser.add_subparsers(dest="command")
    brief = commands.add_parser("brief", help="проверить обязательные поля брифа")
    brief.add_argument("--order", required=True, help="идентификатор заказа")
    readiness = commands.add_parser("readiness", help="проверить готовность этапа к согласованию")
    readiness.add_argument("--stage", required=True)
    overdue = commands.add_parser("overdue", help="найти просроченные задачи по сроку")
    overdue.add_argument("--order", help="ограничить поиск одним заказом")
    overdue.add_argument("--as-of", default=date.today().isoformat(), help="дата проверки YYYY-MM-DD")
    summary = commands.add_parser("summary", help="сформировать сводку по заказу")
    summary.add_argument("--order", required=True)
    summary.add_argument("--as-of", default=date.today().isoformat())
    demo = commands.add_parser("demo", help="показать все четыре операции для заказа")
    demo.add_argument("--order", default="O1")
    demo.add_argument("--as-of", default=date.today().isoformat())
    return parser


def run_command(args, data):
    if args.command == "brief":
        order = get_record(data["orders"], args.order, "Заказ")
        return check_brief(order)
    if args.command == "readiness":
        return check_stage_readiness(data, args.stage)
    if args.command == "overdue":
        return find_overdue_tasks(data, args.as_of, args.order)
    if args.command == "summary":
        return build_order_summary(data, args.order, args.as_of)
    order = get_record(data["orders"], args.order, "Заказ")
    return {
        "brief": check_brief(order),
        "readiness": [check_stage_readiness(data, stage["id"]) for stage in data["stages"] if stage["order_id"] == args.order],
        "overdue": find_overdue_tasks(data, args.as_of, args.order),
        "summary": build_order_summary(data, args.order, args.as_of),
    }


def print_result(command, result):
    if command == "brief":
        print(f"Бриф {result['order_id']}: {'заполнен' if result['complete'] else 'неполный'}")
        for error in result["errors"]:
            print(f"  - {error}")
    elif command == "readiness":
        state = "готов к согласованию" if result["ready"] else "не готов к согласованию"
        print(f"Этап {result['stage_id']}: {state}")
        for version in result["versions"]:
            print(f"  Материал {version['material_id']}: версия {version['version_id']}")
        for blocker in result["blockers"]:
            print(f"  - {blocker}")
    elif command == "overdue":
        print(f"Просроченные задачи на {result['as_of']}: {len(result['tasks'])}")
        for task in result["tasks"]:
            print(f"  {task['task_id']} | {task['title']} | срок {task['deadline']} | просрочка {task['days_overdue']} дн.")
    elif command == "summary":
        order = result["order"]
        totals = result["totals"]
        print(f"Сводка заказа {order['id']} на {result['as_of']}: {order['title']}")
        print(f"  Клиент: {result['client']['name']}; статус заказа: {order['status']}")
        print(f"  Этапов: {totals['stages']}; задач: {totals['tasks']}; завершено: {totals['done_tasks']}")
        print(f"  Материалов: {totals['materials']}; версий: {totals['versions']}; решений клиента: {totals['approvals']}")
        print(f"  Открытых замечаний: {totals['open_comments']}; просроченных задач: {totals['overdue_tasks']}")
        for member in result["team"]:
            print(f"  {member['name']}: {member['project_role']}")
        for approval in result["approvals"]:
            print(f"  Решение {approval['id']} по версии {approval['version_id']}: {approval['decision']} ({approval['decided_on']})")
    else:
        print_result("brief", result["brief"])
        for readiness in result["readiness"]:
            print_result("readiness", readiness)
        print_result("overdue", result["overdue"])
        print_result("summary", result["summary"])


def main(argv=None):
    args = make_parser().parse_args(argv)
    try:
        result = run_command(args, load_data(args.data))
    except (OSError, UnicodeError, ValueError) as error:
        print(f"Ошибка: {error}", file=sys.stderr)
        return 2
    if args.json:
        print(json.dumps(result, ensure_ascii=False, indent=2))
    else:
        print_result(args.command, result)
    return 0


if __name__ == "__main__":
    sys.exit(main())
