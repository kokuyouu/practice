package core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final Map<String, Client> clientsById;
    private final Map<String, Order> ordersById;
    private final Map<String, Stage> stagesById;
    private final Map<String, Task> tasksById;
    private final Map<String, Material> materialsById;
    private final Map<String, MaterialVersion> versionsById;
    private final Map<String, Comment> commentsById;
    private final Map<String, Set<String>> membersByOrder;

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

        clientsById = index(this.clients, Client::id);
        ordersById = index(this.orders, Order::id);
        stagesById = index(this.stages, Stage::id);
        tasksById = index(this.tasks, Task::id);
        materialsById = index(this.materials, Material::id);
        versionsById = index(this.materialVersions, MaterialVersion::id);
        commentsById = index(this.comments, Comment::id);
        Map<String, Set<String>> memberships = new HashMap<>();
        for (OrderMember member : this.orderMembers) {
            memberships.computeIfAbsent(member.orderId(), ignored -> new HashSet<>())
                    .add(member.employeeId());
        }
        memberships.replaceAll((orderId, employeesInOrder) -> Set.copyOf(employeesInOrder));
        membersByOrder = Map.copyOf(memberships);
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
        return find(clientsById, id, "Клиент");
    }

    public Order order(String id) {
        return find(ordersById, id, "Заказ");
    }

    public Stage stage(String id) {
        return find(stagesById, id, "Этап");
    }

    public Task task(String id) {
        return find(tasksById, id, "Задача");
    }

    public Material material(String id) {
        return find(materialsById, id, "Материал");
    }

    public MaterialVersion version(String id) {
        return find(versionsById, id, "Версия");
    }

    public Comment comment(String id) {
        return find(commentsById, id, "Замечание");
    }

    public boolean isOrderMember(String orderId, String employeeId) {
        if (orderId == null || employeeId == null) {
            return false;
        }
        return membersByOrder.getOrDefault(orderId, Set.of()).contains(employeeId);
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

    private static <T> Map<String, T> index(List<T> records, Function<T, String> identifier) {
        Map<String, T> result = new HashMap<>();
        for (T record : records) {
            result.putIfAbsent(identifier.apply(record), record);
        }
        return Map.copyOf(result);
    }

    private static <T> T find(Map<String, T> records, String id, String entity) {
        T record = id == null ? null : records.get(id);
        if (record == null) {
            throw new IllegalArgumentException(entity + ": запись '" + id + "' не найдена");
        }
        return record;
    }
}
