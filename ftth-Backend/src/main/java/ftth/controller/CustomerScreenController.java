package ftth.controller;
import java.util.List;
import java.util.Scanner;
import ftth.service.*;
import ftth.util.InputUtil;
import ftth.model.*;
public class CustomerScreenController {
    private final CustomerService customerService;
    private final BillService billService;
    private final EmailService emailService;
    private final CustomerConnectionService customerConnectionService;
    private final ServiceAreaService serviceAreaService;
    private final PlanService planService;
    private final InventoryService inventoryService;
    public CustomerScreenController(ServiceAreaService serviceAreaService,CustomerService customerService,BillService billService,EmailService emailService,PlanService planService,CustomerConnectionService customerConnectionService,InventoryService inventoryService) {
        this.serviceAreaService = serviceAreaService;
        this.customerService = customerService;
        this.billService = billService;
        this.emailService =emailService;
        this.planService=planService;
        this.customerConnectionService=customerConnectionService;
        this.inventoryService=inventoryService;
    }

    // ============================
    // ENTRY FROM ADMIN CONTROLLER
    // ============================
    public void menu(Scanner sc,User currentUser) {

        System.out.println("\n--- Customer Lookup ---");
        System.out.println("[1] Look up by Customer Code");
        System.out.println("[2] List all customers");
        System.out.print("Choose: ");

        String choice = sc.nextLine().trim();

        switch (choice) {
            case "1":
                lookupById(sc,currentUser);
                break;
            case "2":
                listAll();
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }

    // ============================
    // STEP 1: LOOKUP
    // ============================
    private void lookupById(Scanner sc,User currentUser) {

        System.out.print("Enter Customer Code: ");
        String customerCode = sc.nextLine().trim().toUpperCase();

        Customer customer =customerService.lookupCustomerByCode(customerCode);

        if (customer == null) {
            System.out.println("No customer found. Returning to menu.");
            return;
        }

        // ✅ SHOW CUSTOMER CARD
        printCustomerCard(customer);

        // ✅ CALL CUSTOMER ACTION MENU
        customerActionMenu(sc, customer,currentUser);
    }

    private void listAll() {
        List<Customer> customers = customerService.listAllCustomers();
        if (customers.isEmpty()) {
            System.out.println("No customers found.");
            return;
        }
        
    printCustomerListHeader();

    for (Customer c : customers) {
        printCustomerRow(c);
    }

    }

    // ============================
    // STEP 2: CUSTOMER ACTION MENU
    // ============================
    private void customerActionMenu(Scanner sc, Customer customer,User currentUser) {

        boolean back = false;

        while (!back) {

            System.out.println("\nWhat would you like to do?");
            System.out.println("[1] Change Plan");
            System.out.println("[2] Move (New Pincode)");
            System.out.println("[3] Billing");
            System.out.println("[4] Disconnect");
            System.out.println("[0] Back to Main Menu");
            System.out.print("Select Option: ");

            String opt = sc.nextLine().trim();

            switch (opt) {
                case "1":
                    changePlan(sc, customer,currentUser);
                    customer = customerService.lookupCustomerByCode(customer.getCustomerCode());
                    printCustomerCard(customer);
                    break;

                case "2":
                    moveCustomer(sc, customer,currentUser);
                    customer = customerService.lookupCustomerByCode(customer.getCustomerCode());
                    printCustomerCard(customer);
                    break;

                case "3":
                    billingMenu(sc, customer);
                    break;

                case "4":
                    disconnectCustomer(sc, customer,currentUser);
                    back = true;
                    break;

                case "0":
                    back = true;
                    break;

                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    // ============================
    // ACTIONS (CALL SERVICE ONLY)
    // ============================
   // ============================
// CHANGE PLAN (CALL SERVICE ONLY)
// ============================
private void changePlan(Scanner sc, Customer customer, User currentUser) {

    // 1️⃣ Get active connection
    CustomerConnection connection =
        customerConnectionService.getActiveConnectionByCustomerCode(
            customer.getCustomerCode()
        );

    if (connection == null) {
        System.out.println("No active connection found for this customer.");
        return;
    }

    // 2️⃣ Get current plan
    Plan currentPlan =planService.findPlanById(connection.getPlanId());
    if (currentPlan == null) {
    System.out.println("Current plan not found.");
    return;
}


    System.out.println(
        "\nCurrent Plan : " +
        currentPlan.getPlanName() +
        " | Speed: " + currentPlan.getSpeedLabel() +
        " | Data: " + currentPlan.getDataLimitLabel() +
        " | OTTs: " + currentPlan.getOttCount() +
        " | OLT: " + currentPlan.getOltType() +
        " | Rs." + currentPlan.getMonthlyPrice()
    );

    // 3️⃣ Filter plans: exclude current, only show plans whose OLT type has ports in this pincode
    Long serviceAreaId = connection.getServiceAreaId();
    List<Plan> plans = planService.getActivePlans();
    plans.removeIf(p -> p.getPlanId().equals(currentPlan.getPlanId())
                     || inventoryService.getAvailablePortsByType(serviceAreaId, p.getOltType()) <= 0);

    if (plans.isEmpty()) {
        System.out.println("No other plans available with ports in this pincode.");
        return;
    }

    System.out.println("\nAvailable Plans:");
    for (Plan p : plans) {
        System.out.println(
            p.getPlanId() + ". " +
            p.getPlanName() +
            " | Speed: " + p.getSpeedLabel() +
            " | Data: " + p.getDataLimitLabel() +
            " | OTTs: " + p.getOttCount() +
            " | Rs." + p.getMonthlyPrice() +
            " | OLT: " + p.getOltType()
        );
    }

    // 4️⃣ Select new plan
    Plan selectedPlan;
    while (true) {
        long planId = InputUtil.readLong(sc, "Select New Plan ID: ");
        selectedPlan = planService.findPlanById(planId);

        if (selectedPlan == null || !selectedPlan.isActive()) {
            System.out.println("Invalid plan ID.");
            continue;
        }

        if (selectedPlan.getPlanId().equals(currentPlan.getPlanId())) {
            System.out.println("Already on this plan. Choose a different one.");
            continue;
        }

        if (inventoryService.getAvailablePortsByType(serviceAreaId, selectedPlan.getOltType()) <= 0) {
            System.out.println("No " + selectedPlan.getOltType() + " ports available. Pick another plan.");
            continue;
        }

        break;
    }

    // 5️⃣ Confirm
    boolean oltChange = !currentPlan.getOltType().equals(selectedPlan.getOltType());
    System.out.println(
        "\nChange from : " +
        currentPlan.getPlanName() +
        " @ Rs." + currentPlan.getMonthlyPrice() +
        " (" + currentPlan.getOltType() + ")"
    );
    System.out.println(
        "Change to   : " +
        selectedPlan.getPlanName() +
        " @ Rs." + selectedPlan.getMonthlyPrice() +
        " (" + selectedPlan.getOltType() + ")"
    );
    if (oltChange) {
        System.out.println("Note: OLT type will change. Port will be reallocated.");
    }

    System.out.print("Confirm change? (y/n): ");
    if (!sc.nextLine().equalsIgnoreCase("y")) {
        System.out.println("Cancelled.");
        return;
    }

    // 6️⃣ Call SERVICE (single responsibility)
    customerConnectionService.changePlan(
        connection.getConnectionId(),
        selectedPlan.getPlanId(),
        currentUser.getUserId()
    );

    System.out.println("✅ Plan changed successfully.");
}

private void moveCustomer(Scanner sc, Customer customer, User currentUser) {

    // 2️⃣ Fetch active connection
    CustomerConnection connection =customerConnectionService.getActiveConnectionByCustomerCode(customer.getCustomerCode());

    if (connection == null) {
        System.out.println("No active connection found.");
        return;
    }
    String pincode=serviceAreaService.getPincode(connection.getServiceAreaId());
    Long planId=connection.getPlanId();
    System.out.println("Current Pincode : " + pincode);
    // 3️⃣ Read new pincode
    long newPincode =
        InputUtil.readLong(sc, "Enter New Pincode (0 to cancel): ");

    if (newPincode == 0) {
        System.out.println("Cancelled.");
        return;
    }

    // 4️⃣ Get OLT type from current plan
    Plan currplan = planService.findPlanById(planId);
    String oltType=currplan.getOltType();

    // 5️⃣ Validate OLT type availability at new pincode
    ServiceArea newArea = serviceAreaService.findByPincode(newPincode);
    if (newArea == null) {
        System.out.println("Service not available in pincode " + newPincode + ".");
        return;
    }
    int availablePorts = inventoryService.getAvailablePortsByType(newArea.getServiceAreaId(), oltType);
    if (availablePorts <= 0) {
        System.out.println("No " + oltType + " ports available in pincode " + newPincode + ".");
        System.out.println("Customer's plan (" + currplan.getPlanName() + ") requires " + oltType + ".");
        return;
    }
    System.out.println("OLT Type (from plan): " + oltType);
    System.out.println("Available " + oltType + " ports in " + newPincode + " : " + availablePorts);

    // 6️⃣ Confirm
    System.out.print("Confirm move? (y/n): ");
    if (!sc.nextLine().equalsIgnoreCase("y")) {
        System.out.println("Cancelled.");
        return;
    }

    // 7️⃣ Delegate to service
    customerConnectionService.updateCustomerConnection(
        connection,
        newPincode,
        oltType,
        currentUser.getUserId()
    );

    System.out.println("[SUCCESS] Customer moved successfully.");
}

private void disconnectCustomer(Scanner sc, Customer customer,User currentUser) {

   Long connectionId=customerConnectionService.findActiveConnectionByCustomerCode(customer.getCustomerCode());

    // 3️⃣ Fetch connection
    CustomerConnection connection =
        customerConnectionService.getConnectionById(connectionId);

    if (connection == null) {
        System.out.println("Connection not found.");
        return;
    }

    if (!connection.isActive()) {
        System.out.println("Connection is already disconnected.");
        return;
    }

    // 4️⃣ Show summary
    System.out.println("\nYou are about to disconnect:");
    System.out.println("  Connection ID : " + connection.getConnectionId());
    System.out.println("  Customer ID   : " + connection.getCustomerId());
    System.out.println("  Service Area  : " + connection.getServiceAreaId());
    System.out.println("  Port ID       : " + connection.getPortId());

    // 5️⃣ Confirm
    System.out.print("Confirm disconnect? (y/n): ");
    if (!sc.nextLine().equalsIgnoreCase("y")) {
        System.out.println("Cancelled.");
        return;
    }

    // 6️⃣ Delegate to service
    customerConnectionService.disconnect(
        connection.getConnectionId(),
        currentUser.getUserId()
    );

    System.out.println("✅ Customer disconnected successfully.");
}

    // ============================
    // BILLING SUB-MENU
    // ============================
    private void billingMenu(Scanner sc, Customer customer) {

        boolean back = false;
        while (!back) {
            System.out.println("\n--- Billing ---");
            System.out.println("[1] Generate Bill");
            System.out.println("[2] View Bills");
            System.out.println("[3] Mark Bill as Paid");
            System.out.println("[0] Back");
            System.out.print("Select Option: ");

            String opt = sc.nextLine().trim();
            switch (opt) {
                case "1": generateBill(sc, customer); break;
                case "2": viewBills(customer); break;
                case "3": markBillPaid(sc, customer); break;
                case "0": back = true; break;
                default: System.out.println("Invalid option.");
            }
        }
    }

    private void generateBill(Scanner sc, Customer customer) {

        CustomerConnection connection =
            customerConnectionService.getActiveConnectionByCustomerCode(
                customer.getCustomerCode()
            );

        if (connection == null) {
            System.out.println("No active connection found.");
            return;
        }

        Plan plan = planService.findPlanById(connection.getPlanId());
        if (plan == null) {
            System.out.println("Plan not found.");
            return;
        }

        System.out.println("\nPlan   : " + plan.getPlanName() + " @ Rs." + plan.getMonthlyPrice());
        System.out.println("GST    : 18%");
        System.out.print("Confirm bill generation? (y/n): ");
        if (!sc.nextLine().equalsIgnoreCase("y")) {
            System.out.println("Cancelled.");
            return;
        }

        Bill bill = billService.generateMonthlyBill(
            customer,
            connection.getConnectionId(),
            plan
        );

        System.out.println("Bill " + bill.getBillNo() + " generated. Total: Rs." + bill.getTotalAmount().setScale(2));
    }

    private void viewBills(Customer customer) {

        List<Bill> bills = billService.getBillsForCustomer(customer.getCustomerId());

        if (bills.isEmpty()) {
            System.out.println("No bills found for this customer.");
            return;
        }

        System.out.println("\n--- Bills for " + customer.getCustomerCode() + " ---");
        System.out.printf("%-8s %-18s %-12s %-12s %-12s %-10s%n",
            "ID", "Bill No", "Bill Date", "Due Date", "Total", "Status");
        System.out.println("-".repeat(75));

        for (Bill b : bills) {
            System.out.printf("%-8d %-18s %-12s %-12s Rs.%-9s %-10s%n",
                b.getBillId(),
                b.getBillNo(),
                b.getBillDate(),
                b.getDueDate(),
                b.getTotalAmount().setScale(2),
                b.getBillStatus());
        }
    }

    private void markBillPaid(Scanner sc, Customer customer) {

        viewBills(customer);

        List<Bill> bills = billService.getBillsForCustomer(customer.getCustomerId());
        if (bills.isEmpty()) return;

        System.out.print("\nEnter Bill ID to mark as PAID: ");
        long billId;
        try {
            billId = Long.parseLong(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid Bill ID.");
            return;
        }

        try {
            billService.payBill(billId);
            System.out.println("Bill " + billId + " marked as PAID.");
        } catch (RuntimeException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }



    // ============================
    // UI HELPER
    // ============================
   private void printCustomerCard(Customer c) {
      customerService.printCustomerCard(c);
}
   private void printCustomerListHeader() {

    System.out.printf(
        "%-12s | %-20s | %-25s | %-10s%n",
        "Customer Code", "Name", "Email", "Status"
    );
    System.out.println("=".repeat(75));
}
private void printCustomerRow(Customer c) {
    System.out.printf(
        "%-12s | %-20s | %-25s | %-10s%n",
        c.getCustomerCode(),
        c.getFullName(),
        c.getEmail(),
        c.getStatus()
    );
}
}

