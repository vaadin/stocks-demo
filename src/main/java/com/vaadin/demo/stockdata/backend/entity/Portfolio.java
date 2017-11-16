package com.vaadin.demo.stockdata.backend.entity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

public class Portfolio {

    private Set<Investment> investments = Collections.emptySet();

    public Set<Investment> getInvestments() {
        return investments;
    }

    public void setInvestments(Set<Investment> investments) {
        this.investments = investments;
    }

    public BigDecimal getCurrentValue(){
        return getInvestments().stream()
                .map(Investment::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
