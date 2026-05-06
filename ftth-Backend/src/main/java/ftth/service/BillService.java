package ftth.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import ftth.model.*;
import ftth.model.enums.BillStatus;
import ftth.repository.*;
import ftth.util.BillUtil;

public class BillService {

    private final BillRepository billRepository;

    public BillService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    /**
     * Generate the FIRST bill for a new connection (pro-rata).
     * If activated on May 5th, charges only 5 days (May 5 to May 10).
     * Bill date = 10th of current month, due date = 10th of current month.
     */
    public Bill generateFirstBill(Long customerId, Long connectionId, Plan plan) {
        LocalDate today = LocalDate.now();
        LocalDate billDate = today.withDayOfMonth(10);
        LocalDate dueDate = billDate;

        // If today is after the 10th, bill cycle is 10th of this month to 10th of next month
        // Pro-rata = days from today to next 10th
        int daysInCycle;
        int daysUsed;

        if (today.getDayOfMonth() <= 10) {
            // Days from today to 10th of this month
            daysUsed = 10 - today.getDayOfMonth();
            daysInCycle = YearMonth.from(today).lengthOfMonth();
        } else {
            // Days from today to 10th of next month
            LocalDate next10th = today.plusMonths(1).withDayOfMonth(10);
            billDate = next10th;
            dueDate = next10th;
            daysUsed = (int) (next10th.toEpochDay() - today.toEpochDay());
            daysInCycle = YearMonth.from(today).lengthOfMonth();
        }

        BigDecimal monthlyPrice = plan.getMonthlyPrice();
        BigDecimal dailyRate = monthlyPrice.divide(BigDecimal.valueOf(daysInCycle), 4, RoundingMode.HALF_UP);
        BigDecimal planCharge = dailyRate.multiply(BigDecimal.valueOf(daysUsed)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gstAmount = planCharge.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);

        Bill bill = new Bill(
            BillUtil.generateBillNo(),
            customerId,
            connectionId,
            billDate,
            dueDate,
            planCharge,
            gstAmount
        );

        billRepository.insert(bill);
        return bill;
    }

    /**
     * Generate a regular monthly bill (full month charge).
     * Bill date = 10th of current/next month, due date = 10th (same).
     */
    public Bill generateMonthlyBill(Customer customer, long connectionId, Plan plan) {
        LocalDate today = LocalDate.now();

        // Bill date is always the 10th
        LocalDate billDate;
        if (today.getDayOfMonth() <= 10) {
            billDate = today.withDayOfMonth(10);
        } else {
            billDate = today.plusMonths(1).withDayOfMonth(10);
        }
        LocalDate dueDate = billDate;

        BigDecimal planCharge = plan.getMonthlyPrice();
        BigDecimal gstAmount = planCharge.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);

        Bill bill = new Bill(
            BillUtil.generateBillNo(),
            customer.getCustomerId(),
            connectionId,
            billDate,
            dueDate,
            planCharge,
            gstAmount
        );

        billRepository.insert(bill);
        return bill;
    }

    /**
     * Generate bill (used by CustomerConnectionService during connection creation).
     */
    public Bill generateBill(Customer customer, CustomerConnection connection, Plan plan) {
        return generateFirstBill(customer.getCustomerId(), connection.getConnectionId(), plan);
    }

    // ===============================
    // READ
    // ===============================
    public Bill getBillById(Long billId) {
        return billRepository.findById(billId);
    }

    public List<Bill> getBillsForCustomer(Long customerId) {
        return billRepository.findByCustomerId(customerId);
    }

    // ===============================
    // UPDATE — PAY BILL
    // ===============================
    public void payBill(Long billId) {
        Bill bill = billRepository.findById(billId);

        if (bill == null) {
            throw new RuntimeException("Bill not found");
        }

        if (bill.getBillStatus() == BillStatus.PAID) {
            throw new RuntimeException("Bill already paid");
        }

        billRepository.markAsPaid(billId);
    }

    // ===============================
    // UPDATE — MARK OVERDUE
    // ===============================
    public void markOverdueIfRequired(long billId) {
        Bill bill = billRepository.findById(billId);

        if (bill == null) {
            throw new RuntimeException("Bill not found");
        }

        if (bill.getBillStatus() == BillStatus.PAID) {
            throw new RuntimeException("Bill is already paid");
        }

        if (bill.getBillStatus() == BillStatus.OVERDUE) {
            throw new RuntimeException("Bill is already overdue");
        }

        billRepository.markAsOverdue(billId);
    }

    // ===============================
    // PRINT BILL (console)
    // ===============================
    public void printBill(Bill bill, Customer customer) {
        System.out.println("\n+==========================================+");
        System.out.println("|         AAHA TELECOM - INVOICE           |");
        System.out.println("+==========================================+");
        System.out.printf("| %-14s : %-24s |%n", "Bill No", bill.getBillNo());
        System.out.printf("| %-14s : %-24s |%n", "Bill Date", bill.getBillDate());
        System.out.printf("| %-14s : %-24s |%n", "Due Date", bill.getDueDate());
        System.out.println("+------------------------------------------+");
        System.out.printf("| %-14s : %-24s |%n", "Customer ID", customer.getCustomerCode());
        System.out.printf("| %-14s : %-24s |%n", "Name", customer.getFullName());
        System.out.println("+------------------------------------------+");
        System.out.printf("| %-14s : Rs %-20s |%n", "Plan Charge", bill.getPlanCharge().setScale(2));
        System.out.printf("| %-14s : Rs %-20s |%n", "GST (18%)", bill.getGstAmount().setScale(2));
        System.out.println("| ---------------------------------------- |");
        System.out.printf("| %-14s : Rs %-20s |%n", "TOTAL", bill.getTotalAmount().setScale(2));
        System.out.println("+==========================================+");
    }
}
