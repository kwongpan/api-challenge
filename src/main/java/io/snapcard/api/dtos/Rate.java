package io.snapcard.api.dtos;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@Data
public class Rate implements Serializable {

    private BigDecimal bid;
    private BigDecimal ask;
    private BigDecimal last;

}
