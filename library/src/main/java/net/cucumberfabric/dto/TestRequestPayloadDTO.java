package net.cucumberfabric.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class TestRequestPayloadDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<String> uniqueIds;
    private final Map<String, String> stringParams;

    public TestRequestPayloadDTO(List<String> uniqueIds, Map<String, String> stringParams) {
        this.uniqueIds = uniqueIds;
        this.stringParams = stringParams;
    }

    public List<String> getUniqueIds() {
        return uniqueIds;
    }

    public Map<String, String> getStringParams() {
        return stringParams;
    }
}
