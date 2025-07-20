package com.rinha.dto;

import java.io.Serializable;

public record StatusResponse(Boolean failing, Integer minResponseTime) implements Serializable {
}
