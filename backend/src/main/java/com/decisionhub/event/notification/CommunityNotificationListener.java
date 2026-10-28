package com.decisionhub.event.notification;

import com.decisionhub.enums.notification.NotificationType;
import com.decisionhub.enums.notification.ReferenceType;
import com.decisionhub.event.MemberPromotedEvent;
import com.decisionhub.event.MembershipApprovedEvent;
import com.decisionhub.event.MembershipRejectedEvent;
import com.decisionhub.service.interfaces.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommunityNotificationListener {

    private final NotificationService notificationService;

    @EventListener
    @Transactional
    public void handleMembershipApproved(MembershipApprovedEvent event) {
        log.info("Handling MembershipApprovedEvent for user ID: {} in community ID: {}", 
                event.getUserId(), event.getCommunityId());

        String title = "Join Request Approved";
        String message = String.format("Your join request to the community '%s' has been approved!", event.getCommunityName());
        String actionUrl = "/communities/" + event.getCommunityId();

        notificationService.createNotification(
                event.getUserId(),
                title,
                message,
                NotificationType.MEMBERSHIP_APPROVED,
                ReferenceType.COMMUNITY,
                event.getCommunityId(),
                actionUrl
        );
    }

    @EventListener
    @Transactional
    public void handleMembershipRejected(MembershipRejectedEvent event) {
        log.info("Handling MembershipRejectedEvent for user ID: {} in community ID: {}", 
                event.getUserId(), event.getCommunityId());

        String title = "Join Request Rejected";
        String message = String.format("Your join request to the community '%s' was not approved.", event.getCommunityName());
        String actionUrl = "/communities/" + event.getCommunityId();

        notificationService.createNotification(
                event.getUserId(),
                title,
                message,
                NotificationType.MEMBERSHIP_REJECTED,
                ReferenceType.COMMUNITY,
                event.getCommunityId(),
                actionUrl
        );
    }

    @EventListener
    @Transactional
    public void handleMemberPromoted(MemberPromotedEvent event) {
        log.info("Handling MemberPromotedEvent for user ID: {} in community ID: {}", 
                event.getUserId(), event.getCommunityId());

        String title = "Promoted to Moderator";
        String message = String.format("You have been promoted to %s in community '%s'.", 
                event.getNewRole(), event.getCommunityName());
        String actionUrl = "/communities/" + event.getCommunityId();

        notificationService.createNotification(
                event.getUserId(),
                title,
                message,
                NotificationType.MEMBER_PROMOTED,
                ReferenceType.COMMUNITY,
                event.getCommunityId(),
                actionUrl
        );
    }
}
