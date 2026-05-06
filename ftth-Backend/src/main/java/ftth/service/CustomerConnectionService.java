package ftth.service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ftth.model.Bill;
import ftth.model.Customer;
import ftth.model.CustomerConnection;
import ftth.model.Plan;
import ftth.model.ServiceArea;
import ftth.model.dtos.AddConnectionRequest;
import ftth.repository.*;
import ftth.util.BillUtil;


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
public CustomerConnection createConnection(AddConnectionRequest req,Long currentUserId) {
    if (req.getSalary() <= 30000) {
       throw new RuntimeException("Salary below eligibility. Minimum salary required: Rs.30,000.");
    }
   Plan selectedPlan;
    long planId = req.getPlanId();
    selectedPlan = planService.findPlanById(planId);
    if (selectedPlan == null || !selectedPlan.isActive()) {
        throw new RuntimeException("Invalid plan ID: " + planId);
    }
    ServiceArea serviceArea =serviceAreaService.findByPincode(req.getPincode());
    if (serviceArea == null) {
         throw new RuntimeException("Service is not available in this pincode.");
    }
    if (!serviceArea.isActive()) {
        throw new RuntimeException("Service area is currently inactive.");
    }
    int ports = inventoryService.getAvailablePortsByType( serviceArea.getServiceAreaId(),req.getOltType());
    if (ports <= 0) {
        throw new RuntimeException("No " + req.getOltType() + " ports available in pincode " + serviceArea.getPincode() + ".");
    }
    Long portId = inventoryService.allocatePort(serviceArea.getServiceAreaId(),req.getOltType());
    Customer customer =customerService.findOrCreateCustomer(req.getCustomerName(),req.getEmail(),req.getSalary());
    CustomerConnection existing = connectionRepo.findActiveByCustomerId(customer.getCustomerId());
    if (existing != null) {
        throw new RuntimeException("Customer already has an active connection (ID: " + existing.getConnectionId() + ")");
    }
    CustomerConnection connection =
    new CustomerConnection(
        customer.getCustomerId(),
        selectedPlan.getPlanId(),
        portId,
        serviceArea.getServiceAreaId(),
        LocalDate.now(),
        10,
        currentUserId
    );
    connectionRepo.insert(connection);
   BigDecimal planCharge = selectedPlan.getMonthlyPrice();
   BigDecimal gstAmount = planCharge.multiply(new BigDecimal("0.18"));
Bill bill = new Bill(
    BillUtil.generateBillNo(),
    customer.getCustomerId(),
    connection.getConnectionId(),
    LocalDate.now(),
    LocalDate.now().plusMonths(1),
    planCharge,
    gstAmount
);
billRepository.insert(bill);
    email.sendConnectionConfirmation(customer, connection, selectedPlan);
    return connection;
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

    // 5️⃣ Notify customer
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



