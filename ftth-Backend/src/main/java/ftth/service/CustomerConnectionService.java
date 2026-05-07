package ftth.service;
import java.time.LocalDate;
import java.util.List;
import ftth.model.Customer;
import ftth.model.CustomerConnection;
import ftth.model.Plan;
import ftth.model.ServiceArea;
import ftth.model.dtos.AddConnectionRequest;
import ftth.repository.*;


public class CustomerConnectionService {
    private final EmailService email;
    private final InventoryService inventoryService;
    private final PlanService planService;
    private final CustomerRepository customerRepo;
    private final BillRepository billRepository;
    private final CustomerConnectionRepository connectionRepo;
    private final ServiceAreaService serviceAreaService;
    private final CustomerService customerService;

    public CustomerConnectionService(CustomerService customerService,CustomerRepository customerRepo,CustomerConnectionRepository customerConnectionRepository,PlanService planService, InventoryService inventoryService,BillRepository billRepository,ServiceAreaService serviceAreaService,EmailService email) {
        this.billRepository=billRepository;
        this.email = email;
        this.inventoryService = inventoryService;
        this.planService = planService;
        this.customerRepo=customerRepo;
        this.customerService=customerService;
        this.connectionRepo = customerConnectionRepository;
        this.serviceAreaService=serviceAreaService;
    }
public void createConnection(AddConnectionRequest req,Long currentUserId) {
    // ===============================
    // 1️⃣ Validate salary (BUSINESS RULE)
    // ===============================
    if (req.getSalary() <= 30000) {
       System.out.println("Salary below eligibility. Minimum salary required: Rs.30,000.");
        return;
    }
    // ===============================
    // 2️⃣ Validate plan (READ: plans)
    // ===============================
   Plan selectedPlan;
    long planId = req.getPlanId();
    selectedPlan = planService.findPlanById(planId);
    if (selectedPlan == null || !selectedPlan.isActive()) {
        throw new RuntimeException("Invalid plan ID: " + planId);
    }
    // =================================
    // 3️⃣ Validate service area (READ: service_areas)
    // =================================
    ServiceArea serviceArea =serviceAreaService.findByPincode(req.getPincode());

    if (serviceArea == null) {
         System.out.println("Service is not available in this pincode.");
         return;
    }

    if (!serviceArea.isActive()) {
        System.out.println("Service area is currently inactive.");
        return;
    }

    // =================================
    // 4️⃣ Allocate inventory (READ + UPDATE)
    // TABLE: inventory / olt_ports
    // =================================
  
    int ports = inventoryService.getAvailablePortsByType( serviceArea.getServiceAreaId(),req.getOltType());
    if (ports <= 0) {
        System.out.println("No " + req.getOltType() + " ports available in pincode " + serviceArea.getPincode() + ".");
        return;
    }
    // 🔥 UPDATE inventory (reduce available ports by 1)
    Long portId = inventoryService.allocatePort(serviceArea.getServiceAreaId(),req.getOltType());

    // =================================
    // 5️⃣ Insert or fetch customer (customers)
    // =================================
    Customer customer =customerService.findOrCreateCustomer(req.getCustomerName(),req.getEmail(),req.getSalary());

    // 5.5️⃣ Check if customer already has an active connection
    CustomerConnection existing = connectionRepo.findActiveByCustomerId(customer.getCustomerId());
    if (existing != null) {
        throw new RuntimeException("Customer already has an active connection (ID: " + existing.getConnectionId() + ")");
    }

    // =================================
    // 6️⃣ Insert customer connection (CORE)
    // TABLE: customer_connections
    // =================================
    CustomerConnection connection =
    new CustomerConnection(
        customer.getCustomerId(),
        selectedPlan.getPlanId(),
        portId,
        serviceArea.getServiceAreaId(),
        LocalDate.now(),   // activated_on
        10,                // billing_day (default)
        currentUserId      // created_by (admin / csr)
    );


    connectionRepo.insert(connection);
    // 🔥 INSERT INTO customer_connections
    // =================================
    // 7️⃣ Create billing entry (pro-rata first bill)
    // =================================
    BillService billService = new BillService(billRepository);
    billService.generateFirstBill(customer.getCustomerId(), connection.getConnectionId(), selectedPlan);
    // =================================
    // 8️⃣ Send confirmation email 
    
    email.sendConnectionConfirmation(customer, connection, selectedPlan);

  
}
public void updateCustomerConnection(CustomerConnection connection,
                         long newPincode,
                         String oltType,
                         Long currentUserId) {

    // 1️⃣ Validate new service area
    ServiceArea newArea =serviceAreaService.getActiveServiceArea(newPincode);
    if(newArea==null){
        System.out.println("Service not Available..");
        return;
    }

    // 2️⃣ Allocate new port
    Long newPortId =inventoryService.allocatePort(newArea.getServiceAreaId(),oltType );

    // 3️⃣ Release old port
    inventoryService.releasePort(connection.getPortId());

    // 4️⃣ Update connection record
    connectionRepo.updateLocation(
        connection.getConnectionId(),
        newArea.getServiceAreaId(),
        newPortId,
        currentUserId
    );

    // 5️⃣ (Optional) Send notification
    Customer customer =customerRepo.findById(connection.getCustomerId());
    email.sendCustomerMoveEmail(customer, connection,newPincode);
}
/**
 * Get active customer connection using customer code.
 */
public CustomerConnection getActiveConnectionByCustomerCode(String customerCode) {

    if (customerCode == null || customerCode.isEmpty()) {
        throw new IllegalArgumentException("Customer code cannot be empty");
    }

    return connectionRepo.findActiveByCustomerCode(customerCode);
}
public void changePlan(Long connectionId,
                       Long newPlanId,
                       Long currentUserId) {

    // 1️⃣ Validate new plan
    Plan newPlan = planService.getActivePlan(newPlanId);

    // 2️⃣ Ensure connection exists and is active
    CustomerConnection connection =connectionRepo.findById(connectionId);

    if (connection == null || !connection.isActive()) {
        throw new RuntimeException("Connection not active or not found");
    }

    Plan oldPlan = planService.findPlanById(connection.getPlanId());

    // 3️⃣ If OLT type changes, reallocate port
    if (!oldPlan.getOltType().equals(newPlan.getOltType())) {
        Long newPortId = inventoryService.allocatePort(
            connection.getServiceAreaId(), newPlan.getOltType()
        );
        inventoryService.releasePort(connection.getPortId());
        connectionRepo.updatePort(connectionId, newPortId, currentUserId);
    }

    // 4️⃣ Update plan
    connectionRepo.updatePlan(
        connectionId,
        newPlan.getPlanId(),
        currentUserId
    );

    // 5️⃣ Generate differential bill (only the price difference for remaining days)
    BillService billService = new BillService(billRepository);
    billService.generatePlanChangeBill(
        connection.getCustomerId(),
        connection.getConnectionId(),
        oldPlan,
        newPlan
    );

    // 6️⃣ Notify customer
    Customer customer =customerRepo.findById(connection.getCustomerId());

    email.sendPlanChangeEmail(
        customer,
        connection,
        oldPlan,
        newPlan
    );
}
public void disconnect(Long connectionId, Long currentUserId) {

    // 1️⃣ Fetch connection
    CustomerConnection connection =
        connectionRepo.findById(connectionId);

    if (connection == null) {
        throw new RuntimeException("Connection not found");
    }

    if (!connection.isActive()) {
        throw new RuntimeException("Connection already disconnected");
    }

    // 2️⃣ Disconnect connection (soft delete)
    connectionRepo.disconnect(
        connectionId,
        LocalDate.now(),
        currentUserId
    );
      Customer customer =customerRepo.findById(connection.getCustomerId());

    // 3️⃣ Release port
    inventoryService.releasePort(connection.getPortId());
    customer.deactivate();

    // 4️⃣ Notify customer (optional)
    email.sendDisconnectionEmail(customer, connection);;
}



public Customer getCustomer(String customerCode) {
    return customerRepo.findByCode(customerCode);
}
public String[] findActiveConnectionByCode(String customerCode) {
    return connectionRepo.findActiveConnectionByCustomerCode(customerCode);
}
public Long findActiveConnectionByCustomerCode(String customerCode) {
    return connectionRepo.findActiveConnectionIdByCustomerCode(customerCode);
}
public Long getActivePlanId(String customerCode) {
    return connectionRepo.findActivePlanIdByCustomerCode(customerCode);
}

public void disconnectConnection(long connectionId, boolean confirm) {
    String[] conn = connectionRepo.findConnection(connectionId);
    if (conn == null) {
        System.out.println("Connection not found.");
        return;
    }
    if ("DISCONNECTED".equals(conn[5])) {
        System.out.println("Connection is already disconnected.");
        return;
    }
    System.out.println("Customer : " + conn[1]);
    System.out.println("Pincode  : " + conn[2]);
    System.out.println("Plan     : " + conn[3] + " @ Rs." + conn[4]);
    System.out.println("Port     : " + conn[6] + "/Spl" + conn[7] + "/Port" + conn[8]);

    if (!confirm) {
        System.out.println("Cancelled.");
        return;
    }

    boolean ok = connectionRepo.disconnectCustomer(connectionId);
    if (ok) {
        System.out.println("Connection " + connectionId + " disconnected. Port is now free.");
    } else {
        System.out.println("Disconnect failed.");
    }
}
public void listActiveConnections() {
    connectionRepo.listActiveConnections();
}
public String[] findConnection(long connectionId) {
    return connectionRepo.findConnection(connectionId);
}
// public void listAllCustomers() {
//     ftth.listAllCustomers();
// }
public List<Customer> listAllCustomers() {
    return customerRepo.findAll();
}

/**admin
 * Fetch a customer connection by connection ID.
 */
public CustomerConnection getConnectionById(Long connectionId) {

    if (connectionId == null) {
        throw new IllegalArgumentException("Connection ID cannot be null");
    }

    return connectionRepo.findById(connectionId);
}

public Customer lookupCustomerById(String customerCode) {

    if (customerCode == null || customerCode.trim().isEmpty()) {
        throw new IllegalArgumentException("Customer ID cannot be empty");
    }

    Customer customer = customerRepo.findByCode(customerCode);

    return customer; // may be null if not found
}
}



