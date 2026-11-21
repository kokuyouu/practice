package core;

import java.util.List;
import java.util.function.Function;

public final class CampaignData {
    private final List<Client> clients;
    private final List<Order> orders;
    private final List<Employee> employees;
    private final List<OrderMember> orderMembers;
    private final List<Stage> stages;
    private final List<Task> tasks;
    private final List<Material> materials;
    private final List<MaterialVersion> materialVersions;
    private final List<TaskMaterial> taskMaterials;
    private final List<Approval> approvals;
    private final List<Comment> comments;

    public CampaignData(
            List<Client> clients,
            List<Order> orders,
            List<Employee> employees,
            List<OrderMember> orderMembers,
            List<Stage> stages,
            List<Task> tasks,
            List<Material> materials,
            List<MaterialVersion> materialVersions,
            List<TaskMaterial> taskMaterials,
            List<Approval> approvals,
            List<Comment> comments) {
        this.clients = List.copyOf(clients);
        this.orders = List.copyOf(orders);
        this.employees = List.copyOf(employees);
        this.orderMembers = List.copyOf(orderMembers);
        this.stages = List.copyOf(stages);
        this.tasks = List.copyOf(tasks);
        this.materials = List.copyOf(materials);
        this.materialVersions = List.copyOf(materialVersions);
        this.taskMaterials = List.copyOf(taskMaterials);
        this.approvals = List.copyOf(approvals);
        this.comments = List.copyOf(comments);
    }

    public List<Client> clients() {
        return clients;
    }

    public List<Order> orders() {
        return orders;
    }

    public List<Employee> employees() {
        return employees;
    }

    public List<OrderMember> orderMembers() {
        return orderMembers;
    }

    public List<Stage> stages() {
        return stages;
    }

    public List<Task> tasks() {
        return tasks;
    }

    public List<Material> materials() {
        return materials;
    }

    public List<MaterialVersion> materialVersions() {
        return materialVersions;
    }

    public List<TaskMaterial> taskMaterials() {
        return taskMaterials;
    }

    public List<Approval> approvals() {
        return approvals;
    }

    public List<Comment> comments() {
        return comments;
    }

    public Client client(String id) {
        return find(clients, id, Client::id, "Клиент");
    }

    public Order order(String id) {
        return find(orders, id, Order::id, "Заказ");
    }

    public Stage stage(String id) {
        return find(stages, id, Stage::id, "Этап");
    }

    public Task task(String id) {
        return find(tasks, id, Task::id, "Задача");
    }

    public Material material(String id) {
        return find(materials, id, Material::id, "Материал");
    }

    public MaterialVersion version(String id) {
        return find(materialVersions, id, MaterialVersion::id, "Версия");
    }

    public Comment comment(String id) {
        return find(comments, id, Comment::id, "Замечание");
    }

    public void validateContext() {
        for (Task task : tasks) {
            task.validateContext(this);
        }
        for (TaskMaterial link : taskMaterials) {
            if (!task(link.taskId()).stageId().equals(material(link.materialId()).stageId())) {
                throw new IllegalArgumentException(
                        "task_materials: задача и материал относятся к разным этапам");
            }
        }
    }

    private static <T> T find(
            List<T> records, String id, Function<T, String> identifier, String entity) {
        for (T record : records) {
            if (identifier.apply(record).equals(id)) {
                return record;
            }
        }
        throw new IllegalArgumentException(entity + ": запись '" + id + "' не найдена");
    }
}
