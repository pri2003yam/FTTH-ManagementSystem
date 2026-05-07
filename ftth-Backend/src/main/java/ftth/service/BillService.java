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

    private static final int BILLING_DAY = 10;
    private static final int DUE_DAYS = 15;

    private final BillRepository billRepository;

    public BillService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    /**
     * Generate the FIRST bill for a new connection (pro-rata).
     *
     * Billing cycle: 10th to 10th.
     * If activated on May 6, next billing date is May 10.
     * Pro-rata days = May 6 to May 10 = 4 days.
     * Charge = (monthlyPrice / daysInCycle) × daysUsed.
     * Bill date = next 10th. Due date = bill date + 15 days.
     */
    public Bill generateFirstBill(Long customerId, Long connectionId, Plan plan) {
        LocalDate today = LocalDate.now();
        LocalDate nextBillDate = getNextBillDate(today);

        // Pro-rata: days from today until the next 10th
        int daysUsed = (int) (nextBillDate.toEpochDay() - today.toEpochDay());
        if (daysUsed <= 0) daysUsed = 1; // minimum 1 day

        int daysInCycle = getCycleDays(today);

        BigDecimal planCharge = proRataCharge(plan.getMonthlyPrice(), daysUsed, daysInCycle);
        BigDecimal gstAmount = gst(planCharge);

        Bill bill = new Bill(
            BillUtil.generateBillNo(),
            customerId,
            connectionId,
            nextBillDate,
            nextBillDate.plusDays(DUE_DAYS),
            planCharge,
            gstAmount
        );

        billRepository.insert(bill);
        return bill;
    }

    /**
     * Generate a differential bill after a plan change.
     *
     * Charges only the DIFFERENCE between new and old plan for remaining days.
     * Upgrade: positive charge. Downgrade: negative (credit note).
     * If difference is zero, no bill is generated.
     */
    public Bill generatePlanChangeBill(Long customerId, Long connectionId, Plan oldPlan, Plan newPlan) {
        BigDecimal priceDiff = newPlan.getMonthlyPrice().subtract(oldPlan.getMonthlyPrice());

        // No bill needed if same price
        if (priceDiff.compareTo(BigDecimal.ZERO) == 0) return null;

        LocalDate today = LocalDate.now();
        LocalDate nextBillDate = getNextBillDate(today);

        int daysRemaining = (int) (nextBillDate.toEpochDay() - today.toEpochDay());
        if (daysRemaining <= 0) daysRemaining = 1;

        int daysInCycle = getCycleDays(today);

        BigDecimal planCharge = proRataCharge(priceDiff, daysRemaining, daysInCycle);
        BigDecimal gstAmount = gst(planCharge);

        Bill bill = new Bill(
            BillUtil.generateBillNo(),
            customerId,
            connectionId,
            nextBillDate,
            nextBillDate.plusDays(DUE_DAYS),
            planCharge,
            gstAmount
        );

        billRepository.insert(bill);
        return bill;
    }

    /**
     * Generate a regular monthly bill (full month charge).
     * Bill date = 10th, due date = 25th (10th + 15 days).
     * Prevents duplicate bills for the same billing period.
     */
    public Bill generateMonthlyBill(Customer customer, long connectionId, Plan plan) {
        LocalDate today = LocalDate.now();
        LocalDate billDate = getNextBillDate(today);

        // Check if a bill already exists for this connection and bill date
        if (billRepository.existsByConnectionAndBillDate(connectionId, billDate)) {
            throw new RuntimeException("Bill already generated for billing period " + billDate);
        }

        LocalDate dueDate = billDate.plusDays(DUE_DAYS);

        BigDecimal planCharge = plan.getMonthlyPrice();
        BigDecimal gstAmount = gst(planCharge);

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
    // HELPERS
    // ===============================

    /**
     * Get the next billing date (10th).
     * If today is on or before the 10th, bill date = 10th of this month.
     * If today is after the 10th, bill date = 10th of next month.
     */
    private LocalDate getNextBillDate(LocalDate today) {
        if (today.getDayOfMonth() <= BILLING_DAY) {
            return today.withDayOfMonth(BILLING_DAY);
        }
        return today.plusMonths(1).withDayOfMonth(BILLING_DAY);
    }

    /**
     * Get the number of days in the current billing cycle.
     * Cycle = previous 10th to next 10th.
     */
    private int getCycleDays(LocalDate today) {
        LocalDate cycleStart;
        LocalDate cycleEnd;

        if (today.getDayOfMonth() <= BILLING_DAY) {
            cycleStart = today.minusMonths(1).withDayOfMonth(BILLING_DAY);
            cycleEnd = today.withDayOfMonth(BILLING_DAY);
        } else {
            cycleStart = today.withDayOfMonth(BILLING_DAY);
            cycleEnd = today.plusMonths(1).withDayOfMonth(BILLING_DAY);
        }

        return (int) (cycleEnd.toEpochDay() - cycleStart.toEpochDay());
    }

    private BigDecimal proRataCharge(BigDecimal monthlyPrice, int days, int cycleDays) {
        BigDecimal dailyRate = monthlyPrice.divide(BigDecimal.valueOf(cycleDays), 4, RoundingMode.HALF_UP);
        return dailyRate.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal gst(BigDecimal amount) {
        return amount.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
    }
}
