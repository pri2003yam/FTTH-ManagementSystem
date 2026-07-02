package ftth.api.ecm;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ecm")
public class EcmApiController {

    private final EcmRepository ecmRepository;

    public EcmApiController(EcmRepository ecmRepository) {
        this.ecmRepository = ecmRepository;
    }

    // Read Product Offerings from ECM
    @GetMapping("/offerings")
    public List<Map<String, Object>> getOfferings() {
        return ecmRepository.findAllProductOfferings();
    }

    // Get a single offering detail from ECM
    @GetMapping("/offerings/{itemCode}")
    public ResponseEntity<Map<String, Object>> getOffering(@PathVariable String itemCode) {
        Map<String, Object> offering = ecmRepository.findProductOffering(itemCode);
        if (offering == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(offering);
    }

    // Full PO -> PS -> CFSS -> RFSS decomposition
    // Used for: Voice+OTT breakdown, OLT auto-selection, BPMN service decomposition
    @GetMapping("/offerings/{itemCode}/decompose")
    public ResponseEntity<Map<String, Object>> decomposeOffering(@PathVariable String itemCode) {
        Map<String, Object> result = ecmRepository.decomposeOffering(itemCode);
        if (result.get("po") == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }

    // DEBUG: Show attribute view columns and raw data for first offering
    @GetMapping("/debug/attributes-schema")
    public Map<String, Object> debugAttributeSchema() {
        return ecmRepository.debugAttributeColumns();
    }
}
