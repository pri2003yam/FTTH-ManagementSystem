# Requirements Document

## Introduction

This document specifies the requirements for integrating Ericsson Order Care (EOC) Order Management and Service Registry into the Aaha Telecom FTTH Management System. A new `ftth-eoc-delegates/` module will be created containing Camunda JavaDelegate implementations that interact with EOC's built-in adapter beans (`shoppingCartService` for TMF773 Shopping Cart operations and `serviceRegistryService` for product/service lifecycle management). These delegates will be deployed as a JAR into EOC's Process Controller (which has Camunda 7.18 built-in) and will cover all four FTTH workflows: New Connection, Plan Change, Disconnect, and Service Move.

## Glossary

- **EOC**: Ericsson Order Care — the commercial order management platform hosting the Process Controller
- **Process_Controller**: EOC's built-in Camunda 7.18 BPM engine running on port 8090 that executes BPMN workflows and provides injected adapter beans
- **ShoppingCart_Adapter**: The `shoppingCartService` Spring bean available in the Process Controller, providing TMF773 Shopping Cart REST interface methods (createShoppingCart, deleteShoppingCart, getShoppingCart, priceShoppingCart, setStateShoppingCart, submitShoppingCart, updateShoppingCart, validateShoppingCart)
- **ServiceRegistry_Adapter**: The `serviceRegistryService` Spring bean available in the Process Controller, providing CRUD operations for products and services (createProduct, getProductById, updateProduct, patchProduct, deleteProduct, searchProducts, createService, getServiceById, updateService, patchService, deleteService, searchServices, getProducts, manageProducts, getServices, manageServices)
- **Delegate_Module**: The `ftth-eoc-delegates/` Maven module packaged as a JAR containing all JavaDelegate implementations, deployed into the Process Controller classpath
- **Shopping_Cart**: A TMF773-compliant order container holding product order items with actions (add, modify, delete) that can be validated, priced, and submitted
- **Product_Order_Item**: A line item within a Shopping Cart representing an FTTH product action (new subscription, plan change, disconnect, or service move)
- **Service_Registry**: EOC's catalog/inventory that stores active products and services with their lifecycle status
- **FTTH_Product**: A registered product in the Service Registry representing a customer's fiber subscription (plan, speed, pricing)
- **FTTH_Service**: A registered service in the Service Registry representing the physical fiber connection (port, OLT, location)
- **OLT**: Optical Line Terminal — network equipment types include OLT300 and OLT500
- **Pincode**: A 6-digit service area identifier used for location-based routing
- **DelegateExecution**: The Camunda process variable context available to JavaDelegate implementations for reading and writing workflow variables
- **SaveOrderInSR_Delegate**: An out-of-the-box EOC JavaDelegate (`saveOrderInSR`) available for bulk product/service creation in the Service Registry

## Requirements

### Requirement 1: Module Structure and Build Configuration

**User Story:** As a developer, I want a well-structured Maven module with correct dependency scoping, so that the JAR can be deployed into the Process Controller classpath without conflicts.

#### Acceptance Criteria

1. THE Delegate_Module SHALL use Maven packaging type `jar` with groupId `com.aaha.ftth` and artifactId `ftth-eoc-delegates`
2. THE Delegate_Module SHALL declare Camunda BPM 7.18 dependencies with `provided` scope
3. THE Delegate_Module SHALL declare Spring Framework dependencies with `provided` scope
4. THE Delegate_Module SHALL declare EOC adapter dependencies with `provided` scope
5. THE Delegate_Module SHALL target Java 17 compilation
6. THE Delegate_Module SHALL include a Spring component scan configuration for the package `com.aaha.ftth.eoc.delegate`

### Requirement 2: Shopping Cart Creation Delegates

**User Story:** As a workflow designer, I want delegates that create Shopping Carts with the correct FTTH product items for each flow type, so that orders are properly initiated in EOC's Order Management.

#### Acceptance Criteria

1. WHEN the New Connection workflow starts, THE CreateNewConnectionCartDelegate SHALL invoke `shoppingCartService.createShoppingCart()` with a product order item containing action "add", the FTTH plan identifier, customer details, and service area pincode from DelegateExecution variables
2. WHEN the Plan Change workflow starts, THE CreatePlanChangeCartDelegate SHALL invoke `shoppingCartService.createShoppingCart()` with a product order item containing action "modify", the new plan identifier, and the existing connection reference from DelegateExecution variables
3. WHEN the Disconnect workflow starts, THE CreateDisconnectCartDelegate SHALL invoke `shoppingCartService.createShoppingCart()` with a product order item containing action "delete" and the existing connection reference from DelegateExecution variables
4. WHEN the Service Move workflow starts, THE CreateServiceMoveCartDelegate SHALL invoke `shoppingCartService.createShoppingCart()` with a product order item containing action "modify", the new pincode, and the existing connection reference from DelegateExecution variables
5. WHEN any cart creation delegate completes successfully, THE Delegate SHALL store the returned Shopping Cart identifier in the DelegateExecution variable `shoppingCartId`
6. IF `shoppingCartService.createShoppingCart()` throws an exception, THEN THE Delegate SHALL throw a BpmnError with error code `ERR_CART_CREATION_FAILED` and include the exception message

### Requirement 3: Shopping Cart Validation Delegate

**User Story:** As a workflow designer, I want a delegate that validates Shopping Cart contents against business rules, so that invalid orders are caught before pricing and submission.

#### Acceptance Criteria

1. WHEN the validate cart step executes, THE ValidateCartDelegate SHALL read the `shoppingCartId` variable from DelegateExecution and invoke `shoppingCartService.validateShoppingCart()` with that identifier
2. WHEN `shoppingCartService.validateShoppingCart()` returns a successful validation result, THE ValidateCartDelegate SHALL set the DelegateExecution variable `cartValid` to `true`
3. WHEN `shoppingCartService.validateShoppingCart()` returns validation errors, THE ValidateCartDelegate SHALL set the DelegateExecution variable `cartValid` to `false` and store the error details in variable `cartValidationErrors`
4. IF `shoppingCartService.validateShoppingCart()` throws an exception, THEN THE ValidateCartDelegate SHALL throw a BpmnError with error code `ERR_CART_VALIDATION_FAILED`

### Requirement 4: Shopping Cart Pricing Delegate

**User Story:** As a workflow designer, I want a delegate that triggers pricing calculation for the Shopping Cart, so that the order has accurate charges before submission.

#### Acceptance Criteria

1. WHEN the price cart step executes, THE PriceCartDelegate SHALL read the `shoppingCartId` variable from DelegateExecution and invoke `shoppingCartService.priceShoppingCart()` with that identifier
2. WHEN `shoppingCartService.priceShoppingCart()` returns a priced cart, THE PriceCartDelegate SHALL store the total price in DelegateExecution variable `cartTotalPrice`
3. IF `shoppingCartService.priceShoppingCart()` throws an exception, THEN THE PriceCartDelegate SHALL throw a BpmnError with error code `ERR_CART_PRICING_FAILED`

### Requirement 5: Shopping Cart Submission Delegate

**User Story:** As a workflow designer, I want a delegate that submits the Shopping Cart to trigger fulfillment, so that the order proceeds through the EOC fulfillment pipeline.

#### Acceptance Criteria

1. WHEN the submit cart step executes, THE SubmitCartDelegate SHALL read the `shoppingCartId` variable from DelegateExecution and invoke `shoppingCartService.submitShoppingCart()` with that identifier
2. WHEN `shoppingCartService.submitShoppingCart()` returns a successful submission, THE SubmitCartDelegate SHALL store the order reference in DelegateExecution variable `orderReference`
3. IF `shoppingCartService.submitShoppingCart()` throws an exception, THEN THE SubmitCartDelegate SHALL throw a BpmnError with error code `ERR_CART_SUBMISSION_FAILED`

### Requirement 6: Service Registry — Product and Service Registration for New Connection

**User Story:** As a workflow designer, I want delegates that register the new FTTH product and service in the Service Registry after successful fulfillment, so that the active subscription is tracked.

#### Acceptance Criteria

1. WHEN the New Connection order is fulfilled successfully, THE RegisterProductDelegate SHALL invoke `serviceRegistryService.createProduct()` with the FTTH plan details (plan identifier, plan name, speed, monthly price, OLT type) and customer reference from DelegateExecution variables
2. WHEN `serviceRegistryService.createProduct()` returns a product identifier, THE RegisterProductDelegate SHALL store the identifier in DelegateExecution variable `registeredProductId`
3. WHEN the product registration completes, THE RegisterServiceDelegate SHALL invoke `serviceRegistryService.createService()` with the physical connection details (port identifier, OLT code, splitter number, service area pincode) and the `registeredProductId` from DelegateExecution variables
4. WHEN `serviceRegistryService.createService()` returns a service identifier, THE RegisterServiceDelegate SHALL store the identifier in DelegateExecution variable `registeredServiceId`
5. IF `serviceRegistryService.createProduct()` throws an exception, THEN THE RegisterProductDelegate SHALL throw a BpmnError with error code `ERR_PRODUCT_REGISTRATION_FAILED`
6. IF `serviceRegistryService.createService()` throws an exception, THEN THE RegisterServiceDelegate SHALL throw a BpmnError with error code `ERR_SERVICE_REGISTRATION_FAILED`

### Requirement 7: Service Registry — Product Update for Plan Change

**User Story:** As a workflow designer, I want a delegate that updates the existing product in the Service Registry when a plan change occurs, so that the registry reflects the current subscription details.

#### Acceptance Criteria

1. WHEN the Plan Change order is fulfilled successfully, THE UpdateProductForPlanChangeDelegate SHALL read the `registeredProductId` variable from DelegateExecution and invoke `serviceRegistryService.updateProduct()` with the new plan details (new plan identifier, plan name, speed, monthly price, OLT type)
2. IF the `registeredProductId` variable is not present in DelegateExecution, THEN THE UpdateProductForPlanChangeDelegate SHALL invoke `serviceRegistryService.searchProducts()` using the customer reference to locate the existing product and then perform the update
3. IF `serviceRegistryService.updateProduct()` throws an exception, THEN THE UpdateProductForPlanChangeDelegate SHALL throw a BpmnError with error code `ERR_PRODUCT_UPDATE_FAILED`

### Requirement 8: Service Registry — Product Termination for Disconnect

**User Story:** As a workflow designer, I want a delegate that updates the product status to terminated in the Service Registry when a disconnect occurs, so that the registry reflects the terminated subscription.

#### Acceptance Criteria

1. WHEN the Disconnect order is fulfilled successfully, THE TerminateProductDelegate SHALL read the `registeredProductId` variable from DelegateExecution and invoke `serviceRegistryService.patchProduct()` with status set to "terminated"
2. WHEN the product status is updated to terminated, THE TerminateProductDelegate SHALL invoke `serviceRegistryService.patchService()` with status set to "inactive" using the `registeredServiceId` from DelegateExecution variables
3. IF the `registeredProductId` variable is not present in DelegateExecution, THEN THE TerminateProductDelegate SHALL invoke `serviceRegistryService.searchProducts()` using the customer reference to locate the existing product
4. IF `serviceRegistryService.patchProduct()` throws an exception, THEN THE TerminateProductDelegate SHALL throw a BpmnError with error code `ERR_PRODUCT_TERMINATION_FAILED`

### Requirement 9: Service Registry — Service Location Update for Service Move

**User Story:** As a workflow designer, I want a delegate that updates the service location in the Service Registry when a service move occurs, so that the registry reflects the new physical connection point.

#### Acceptance Criteria

1. WHEN the Service Move order is fulfilled successfully, THE UpdateServiceLocationDelegate SHALL read the `registeredServiceId` variable from DelegateExecution and invoke `serviceRegistryService.updateService()` with the new location details (new port identifier, new OLT code, new splitter number, new service area pincode)
2. IF the `registeredServiceId` variable is not present in DelegateExecution, THEN THE UpdateServiceLocationDelegate SHALL invoke `serviceRegistryService.searchServices()` using the customer reference and existing product identifier to locate the service
3. IF `serviceRegistryService.updateService()` throws an exception, THEN THE UpdateServiceLocationDelegate SHALL throw a BpmnError with error code `ERR_SERVICE_LOCATION_UPDATE_FAILED`

### Requirement 10: BPMN Process Definitions for EOC Process Controller

**User Story:** As a workflow designer, I want BPMN process definitions for all four FTTH flows that reference the EOC delegates, so that the workflows can be deployed into the Process Controller.

#### Acceptance Criteria

1. THE Delegate_Module SHALL include a BPMN process definition for New Connection with service tasks referencing: CreateNewConnectionCartDelegate → ValidateCartDelegate → PriceCartDelegate → SubmitCartDelegate → RegisterProductDelegate → RegisterServiceDelegate
2. THE Delegate_Module SHALL include a BPMN process definition for Plan Change with service tasks referencing: CreatePlanChangeCartDelegate → ValidateCartDelegate → PriceCartDelegate → SubmitCartDelegate → UpdateProductForPlanChangeDelegate
3. THE Delegate_Module SHALL include a BPMN process definition for Disconnect with service tasks referencing: CreateDisconnectCartDelegate → SubmitCartDelegate → TerminateProductDelegate
4. THE Delegate_Module SHALL include a BPMN process definition for Service Move with service tasks referencing: CreateServiceMoveCartDelegate → ValidateCartDelegate → SubmitCartDelegate → UpdateServiceLocationDelegate
5. WHEN a cart validation fails (cartValid equals false), THE BPMN process SHALL route to an error end event without proceeding to pricing or submission
6. WHEN any delegate throws a BpmnError, THE BPMN process SHALL catch the error using a boundary error event and route to an appropriate error handling path

### Requirement 11: Process Variable Mapping and Data Transfer

**User Story:** As a developer, I want a consistent pattern for reading existing FTTH system data from process variables and passing it to EOC adapters, so that the delegates integrate cleanly with upstream workflow steps.

#### Acceptance Criteria

1. THE Delegate_Module SHALL read customer data from DelegateExecution variables: `customerName`, `email`, `customerCode`, `customerId`
2. THE Delegate_Module SHALL read connection data from DelegateExecution variables: `connectionId`, `planId`, `planName`, `oltType`, `monthlyPrice`, `portId`, `oltCode`, `splitterNumber`, `pincode`
3. THE Delegate_Module SHALL read flow-specific data from DelegateExecution variables: `newPlanId` (Plan Change), `newPincode` (Service Move), `newPortId` and `newOltCode` (Service Move)
4. IF a required DelegateExecution variable is missing or null, THEN THE Delegate SHALL throw a BpmnError with error code `ERR_MISSING_VARIABLE` and include the missing variable name in the error message

### Requirement 12: Configuration and Placeholder Management

**User Story:** As a developer deploying into EOC, I want externalized configuration with placeholder values for environment-specific identifiers, so that the JAR can be configured for different EOC environments.

#### Acceptance Criteria

1. THE Delegate_Module SHALL externalize the FTTH product specification identifier as a configurable property with a placeholder value
2. THE Delegate_Module SHALL externalize the FTTH service specification identifier as a configurable property with a placeholder value
3. THE Delegate_Module SHALL externalize the product catalog identifier as a configurable property with a placeholder value
4. THE Delegate_Module SHALL provide a Spring `@Configuration` class that makes all placeholder values available for injection into delegate classes using `@Value` annotations
