# Implementation Plan: EOC Order Management & Service Registry Delegates

## Overview

This plan implements the `ftth-eoc-delegates/` Maven module containing Camunda JavaDelegate implementations that interact with EOC's adapter beans (`shoppingCartService` and `serviceRegistryService`). The delegates are deployed as a JAR into EOC's Process Controller and cover all four FTTH workflows: New Connection, Plan Change, Disconnect, and Service Move.

## Tasks

- [x] 1. Set up module structure, base class, and configuration
  - [x] 1.1 Create the `ftth-eoc-delegates/` Maven module with `pom.xml`
    - Create directory `ftth-eoc-delegates/` at the project root
    - Create `pom.xml` with groupId `com.aaha.ftth`, artifactId `ftth-eoc-delegates`, packaging `jar`
    - Declare Camunda BPM 7.18, Spring Framework 5.3.31 dependencies with `provided` scope
    - Configure Java 17 compilation via `maven-compiler-plugin`
    - Add JUnit 5, Mockito test dependencies
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 1.2 Create `AbstractEocDelegate` base class
    - Create `src/main/java/com/aaha/ftth/eoc/delegate/AbstractEocDelegate.java`
    - Implement `JavaDelegate` interface
    - Add `getRequiredVariable(DelegateExecution, String, Class<T>)` that throws `BpmnError("ERR_MISSING_VARIABLE")` when value is null
    - Add `getRequiredString()` and `getRequiredLong()` convenience methods
    - Add `getOptionalVariable()` method
    - _Requirements: 11.4_

  - [x] 1.3 Create `EocDelegateConfig` configuration class
    - Create `src/main/java/com/aaha/ftth/eoc/delegate/config/EocDelegateConfig.java`
    - Annotate with `@Configuration` and `@ComponentScan("com.aaha.ftth.eoc.delegate")`
    - Add `@Value` fields for `eoc.ftth.product-spec-id`, `eoc.ftth.service-spec-id`, `eoc.ftth.catalog-id` with placeholder defaults
    - Provide getter methods for each property
    - _Requirements: 1.6, 12.1, 12.2, 12.3, 12.4_

  - [x] 1.4 Create `application.properties` with placeholder configuration
    - Create `src/main/resources/application.properties`
    - Add placeholder entries: `eoc.ftth.product-spec-id=PLACEHOLDER_PRODUCT_SPEC`, `eoc.ftth.service-spec-id=PLACEHOLDER_SERVICE_SPEC`, `eoc.ftth.catalog-id=PLACEHOLDER_CATALOG`
    - _Requirements: 12.1, 12.2, 12.3_

- [x] 2. Implement Shopping Cart creation delegates
  - [x] 2.1 Implement `CreateNewConnectionCartDelegate`
    - Create `src/main/java/com/aaha/ftth/eoc/delegate/cart/CreateNewConnectionCartDelegate.java`
    - Annotate as `@Component("createNewConnectionCartDelegate")`
    - Extend `AbstractEocDelegate`, autowire `shoppingCartService` and `EocDelegateConfig`
    - In `execute()`: read `customerName`, `email`, `planId`, `pincode`, `oltType`, `customerCode` from DelegateExecution
    - Build TMF773 cart payload with action "add", product characteristics, and related party
    - Invoke `shoppingCartService.createShoppingCart()`, store returned ID in variable `shoppingCartId`
    - On exception: throw `BpmnError("ERR_CART_CREATION_FAILED", message)`
    - _Requirements: 2.1, 2.5, 2.6, 11.1, 11.2_

  - [x] 2.2 Implement `CreatePlanChangeCartDelegate`
    - Create `src/main/java/com/aaha/ftth/eoc/delegate/cart/CreatePlanChangeCartDelegate.java`
    - Annotate as `@Component("createPlanChangeCartDelegate")`
    - In `execute()`: read `connectionId`, `customerId`, `newPlanId` from DelegateExecution
    - Build cart payload with action "modify" and new plan reference
    - Invoke `shoppingCartService.createShoppingCart()`, store returned ID in variable `shoppingCartId`
    - On exception: throw `BpmnError("ERR_CART_CREATION_FAILED", message)`
    - _Requirements: 2.2, 2.5, 2.6, 11.2, 11.3_

  - [x] 2.3 Implement `CreateDisconnectCartDelegate`
    - Create `src/main/java/com/aaha/ftth/eoc/delegate/cart/CreateDisconnectCartDelegate.java`
    - Annotate as `@Component("createDisconnectCartDelegate")`
    - In `execute()`: read `connectionId`, `customerId` from DelegateExecution
    - Build cart payload with action "delete" and existing connection reference
    - Invoke `shoppingCartService.createShoppingCart()`, store returned ID in variable `shoppingCartId`
    - On exception: throw `BpmnError("ERR_CART_CREATION_FAILED", message)`
    - _Requirements: 2.3, 2.5, 2.6, 11.2_

  - [x] 2.4 Implement `CreateServiceMoveCartDelegate`
    - Create `src/main/java/com/aaha/ftth/eoc/delegate/cart/CreateServiceMoveCartDelegate.java`
    - Annotate as `@Component("createServiceMoveCartDelegate")`
    - In `execute()`: read `connectionId`, `customerId`, `newPincode`, `newPortId`, `newOltCode` from DelegateExecution
    - Build cart payload with action "modify" and new location details
    - Invoke `shoppingCartService.createShoppingCart()`, store returned ID in variable `shoppingCartId`
    - On exception: throw `BpmnError("ERR_CART_CREATION_FAILED", message)`
    - _Requirements: 2.4, 2.5, 2.6, 11.2, 11.3_

  - [x] 2.5 Write unit tests for Shopping Cart creation delegates
    - Create test class for each cart creation delegate
    - Test successful cart creation stores `shoppingCartId` in execution
    - Test exception handling throws correct `BpmnError`
    - Test missing required variable throws `ERR_MISSING_VARIABLE`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 11.4_

- [x] 3. Implement Shopping Cart validation, pricing, and submission delegates
  - [x] 3.1 Implement `ValidateCartDelegate`
    - Create `src/main/java/com/aaha/ftth/eoc/delegate/cart/ValidateCartDelegate.java`
    - Annotate as `@Component("validateCartDelegate")`
    - In `execute()`: read `shoppingCartId`, invoke `shoppingCartService.validateShoppingCart()`
    - On success: set `cartValid` to `true`
    - On validation errors: set `cartValid` to `false`, set `cartValidationErrors` with error details
    - On exception: throw `BpmnError("ERR_CART_VALIDATION_FAILED")`
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 3.2 Implement `PriceCartDelegate`
    - Create `src/main/java/com/aaha/ftth/eoc/delegate/cart/PriceCartDelegate.java`
    - Annotate as `@Component("priceCartDelegate")`
    - In `execute()`: read `shoppingCartId`, invoke `shoppingCartService.priceShoppingCart()`
    - Store total price in `cartTotalPrice` variable
    - On exception: throw `BpmnError("ERR_CART_PRICING_FAILED")`
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 3.3 Implement `SubmitCartDelegate`
    - Create `src/main/java/com/aaha/ftth/eoc/delegate/cart/SubmitCartDelegate.java`
    - Annotate as `@Component("submitCartDelegate")`
    - In `execute()`: read `shoppingCartId`, invoke `shoppingCartService.submitShoppingCart()`
    - Store order reference in `orderReference` variable
    - On exception: throw `BpmnError("ERR_CART_SUBMISSION_FAILED")`
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 3.4 Write unit tests for validate, price, and submit delegates
    - Test ValidateCartDelegate sets `cartValid=true` on success
    - Test ValidateCartDelegate sets `cartValid=false` and `cartValidationErrors` on validation failure
    - Test PriceCartDelegate stores `cartTotalPrice`
    - Test SubmitCartDelegate stores `orderReference`
    - Test all delegates throw correct `BpmnError` on exceptions
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3_

- [x] 4. Checkpoint - Verify Shopping Cart delegates compile and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement Service Registry delegates for New Connection
  - [x] 5.1 Implement `RegisterProductDelegate`
    - Create `src/main/java/com/aaha/ftth/eoc/delegate/registry/RegisterProductDelegate.java`
    - Annotate as `@Component("registerProductDelegate")`
    - Extend `AbstractEocDelegate`, autowire `serviceRegistryService` and `EocDelegateConfig`
    - In `execute()`: read `planId`, `planName`, `monthlyPrice`, `oltType`, `customerCode` from DelegateExecution
    - Build product payload with product specification, characteristics, related party, status "active"
    - Invoke `serviceRegistryService.createProduct()`, store returned ID in `registeredProductId`
    - On exception: throw `BpmnError("ERR_PRODUCT_REGISTRATION_FAILED")`
    - _Requirements: 6.1, 6.2, 6.5, 11.1, 11.2_

  - [x] 5.2 Implement `RegisterServiceDelegate`
    - Create `src/main/java/com/aaha/ftth/eoc/delegate/registry/RegisterServiceDelegate.java`
    - Annotate as `@Component("registerServiceDelegate")`
    - In `execute()`: read `portId`, `oltCode`, `splitterNumber`, `pincode`, `registeredProductId`, `customerCode` from DelegateExecution
    - Build service payload with service specification, characteristics, supporting product, related party, status "active"
    - Invoke `serviceRegistryService.createService()`, store returned ID in `registeredServiceId`
    - On exception: throw `BpmnError("ERR_SERVICE_REGISTRATION_FAILED")`
    - _Requirements: 6.3, 6.4, 6.6, 11.2_

  - [x] 5.3 Write unit tests for RegisterProductDelegate and RegisterServiceDelegate
    - Test successful product creation stores `registeredProductId`
    - Test successful service creation stores `registeredServiceId`
    - Test correct BpmnError thrown on adapter exceptions
    - Test missing variables throw `ERR_MISSING_VARIABLE`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [x] 6. Implement Service Registry delegates for Plan Change, Disconnect, and Service Move
  - [x] 6.1 Implement `UpdateProductForPlanChangeDelegate`
    - Create `src/main/java/com/aaha/ftth/eoc/delegate/registry/UpdateProductForPlanChangeDelegate.java`
    - Annotate as `@Component("updateProductForPlanChangeDelegate")`
    - In `execute()`: attempt to read `registeredProductId`; if missing, fall back to `serviceRegistryService.searchProducts()` using `customerCode`
    - Build update payload with new plan details (`newPlanId`, `planName`, `monthlyPrice`, `oltType`)
    - Invoke `serviceRegistryService.updateProduct()`
    - On exception: throw `BpmnError("ERR_PRODUCT_UPDATE_FAILED")`
    - _Requirements: 7.1, 7.2, 7.3, 11.3_

  - [x] 6.2 Implement `TerminateProductDelegate`
    - Create `src/main/java/com/aaha/ftth/eoc/delegate/registry/TerminateProductDelegate.java`
    - Annotate as `@Component("terminateProductDelegate")`
    - In `execute()`: attempt to read `registeredProductId`; if missing, fall back to `serviceRegistryService.searchProducts()` using `customerCode`
    - Invoke `serviceRegistryService.patchProduct()` with status "terminated"
    - Read `registeredServiceId` and invoke `serviceRegistryService.patchService()` with status "inactive"
    - On exception: throw `BpmnError("ERR_PRODUCT_TERMINATION_FAILED")`
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [x] 6.3 Implement `UpdateServiceLocationDelegate`
    - Create `src/main/java/com/aaha/ftth/eoc/delegate/registry/UpdateServiceLocationDelegate.java`
    - Annotate as `@Component("updateServiceLocationDelegate")`
    - In `execute()`: attempt to read `registeredServiceId`; if missing, fall back to `serviceRegistryService.searchServices()` using `customerCode`
    - Build update payload with new location details (`newPincode`, `newPortId`, `newOltCode`, `newSplitterNumber`)
    - Invoke `serviceRegistryService.updateService()`
    - On exception: throw `BpmnError("ERR_SERVICE_LOCATION_UPDATE_FAILED")`
    - _Requirements: 9.1, 9.2, 9.3, 11.3_

  - [x] 6.4 Write unit tests for Plan Change, Disconnect, and Service Move registry delegates
    - Test UpdateProductForPlanChangeDelegate with direct `registeredProductId` and with fallback search
    - Test TerminateProductDelegate patches product to "terminated" and service to "inactive"
    - Test UpdateServiceLocationDelegate with direct `registeredServiceId` and with fallback search
    - Test correct BpmnError thrown on adapter exceptions for each delegate
    - _Requirements: 7.1, 7.2, 7.3, 8.1, 8.2, 8.3, 8.4, 9.1, 9.2, 9.3_

- [x] 7. Checkpoint - Verify all delegates compile and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Create BPMN process definitions
  - [x] 8.1 Create `EOC_FTTH_New_Connection.bpmn`
    - Create `src/main/resources/bpmn/EOC_FTTH_New_Connection.bpmn`
    - Define service tasks with `camunda:delegateExpression` referencing: `createNewConnectionCartDelegate` → `validateCartDelegate` → `priceCartDelegate` → `submitCartDelegate` → `registerProductDelegate` → `registerServiceDelegate`
    - Add exclusive gateway after ValidateCartDelegate checking `${cartValid == true}`
    - Route to error end event when `cartValid` is false
    - Add boundary error events on all service tasks catching BpmnError by error code
    - _Requirements: 10.1, 10.5, 10.6_

  - [x] 8.2 Create `EOC_FTTH_Plan_Change.bpmn`
    - Create `src/main/resources/bpmn/EOC_FTTH_Plan_Change.bpmn`
    - Define service tasks: `createPlanChangeCartDelegate` → `validateCartDelegate` → `priceCartDelegate` → `submitCartDelegate` → `updateProductForPlanChangeDelegate`
    - Add validation gateway and boundary error events
    - _Requirements: 10.2, 10.5, 10.6_

  - [x] 8.3 Create `EOC_FTTH_Disconnect.bpmn`
    - Create `src/main/resources/bpmn/EOC_FTTH_Disconnect.bpmn`
    - Define service tasks: `createDisconnectCartDelegate` → `submitCartDelegate` → `terminateProductDelegate`
    - Add boundary error events on all service tasks
    - _Requirements: 10.3, 10.6_

  - [x] 8.4 Create `EOC_FTTH_Service_Move.bpmn`
    - Create `src/main/resources/bpmn/EOC_FTTH_Service_Move.bpmn`
    - Define service tasks: `createServiceMoveCartDelegate` → `validateCartDelegate` → `submitCartDelegate` → `updateServiceLocationDelegate`
    - Add validation gateway and boundary error events
    - _Requirements: 10.4, 10.5, 10.6_

- [x] 9. Final checkpoint - Ensure all tests pass and module builds successfully
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Unit tests use Mockito to mock the `shoppingCartService` and `serviceRegistryService` adapter beans
- The module has no embedded server — it's a plain JAR deployed into EOC's Process Controller classpath
- All Spring/Camunda dependencies are `provided` scope since the Process Controller supplies them at runtime

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.3", "1.4"] },
    { "id": 2, "tasks": ["2.1", "2.2", "2.3", "2.4"] },
    { "id": 3, "tasks": ["2.5", "3.1", "3.2", "3.3"] },
    { "id": 4, "tasks": ["3.4", "5.1", "5.2"] },
    { "id": 5, "tasks": ["5.3", "6.1", "6.2", "6.3"] },
    { "id": 6, "tasks": ["6.4"] },
    { "id": 7, "tasks": ["8.1", "8.2", "8.3", "8.4"] }
  ]
}
```
