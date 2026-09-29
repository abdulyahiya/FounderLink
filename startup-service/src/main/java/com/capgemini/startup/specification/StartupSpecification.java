package com.capgemini.startup.specification;

import org.springframework.data.jpa.domain.Specification;

import com.capgemini.startup.entity.Startup;
import com.capgemini.startup.enums.StartupStage;

import java.math.BigDecimal;

public class StartupSpecification {

    private StartupSpecification() {
    }

    public static Specification<Startup> isApproved() {
        return (root, query, cb) -> cb.isTrue(root.get("isApproved"));
    }

    public static Specification<Startup> hasIndustry(String industry) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("industry")), industry.toLowerCase());
    }

    public static Specification<Startup> hasStage(StartupStage stage) {
        return (root, query, cb) -> cb.equal(root.get("stage"), stage);
    }

    public static Specification<Startup> hasMinFunding(BigDecimal minFunding) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fundingGoal"), minFunding);
    }

    public static Specification<Startup> hasMaxFunding(BigDecimal maxFunding) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fundingGoal"), maxFunding);
    }

    public static Specification<Startup> hasLocation(String location) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("location")), location.toLowerCase());
    }
}
