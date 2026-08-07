package com.seerah.review.adapter.in.web;

import com.seerah.review.api.ReviewRegistrar;
import com.seerah.shared.EntityType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Inbound web adapter for scholarly sign-off. */
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewRegistrar registrar;

    public ReviewController(ReviewRegistrar registrar) {
        this.registrar = registrar;
    }

    public record ApproveRequest(UUID targetId, int version, String scholarEmail,
                                 String scholarName, String note) { }

    @PostMapping("/events/approve")
    public ResponseEntity<Void> approveEvent(@RequestBody ApproveRequest r) {
        registrar.approve(EntityType.EVENT, r.targetId(), r.version(),
                r.scholarEmail(), r.scholarName(), r.note());
        return ResponseEntity.noContent().build();
    }
}
