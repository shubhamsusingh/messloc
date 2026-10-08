package com.MessLoc.service;

import com.MessLoc.dto.mess.CreateMessRequest;
import com.MessLoc.dto.mess.MessResponse;
import com.MessLoc.dto.mess.PublicMessResponse;
import com.MessLoc.dto.mess.UpdateMessRequest;

import java.util.List;

public interface MessService {
    
    // OWNER Operations
    MessResponse createMess(CreateMessRequest request, String ownerId);
    List<MessResponse> getOwnerMesses(String ownerId);
    MessResponse getOwnerMess(String id, String ownerId);
    MessResponse updateMess(String id, UpdateMessRequest request, String ownerId);
    void deactivateMess(String id, String ownerId);
    void activateMess(String id, String ownerId);
    void deleteMess(String id, String ownerId);

    // PUBLIC Operations
    List<PublicMessResponse> getPublicMesses();
    PublicMessResponse getPublicMess(String id);

    // ADMIN Operations
    List<MessResponse> getAllMesses();
    List<MessResponse> getPendingMesses();
    void approveMess(String id);
    void rejectMess(String id);
}
