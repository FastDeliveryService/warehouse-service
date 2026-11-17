package com.fds.warehouseservice.service.darkstores;

import com.fds.warehouseservice.dto.darkstores.DarkstoreRequest;
import com.fds.warehouseservice.dto.darkstores.DarkstoreResponse;
import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.mapper.darkstores.DarkstoreMapper;
import com.fds.warehouseservice.repository.darkstores.DarkstoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DarkstoreServiceImpl implements DarkstoreService {

    private final DarkstoreRepository darkstoreRepository;
    private final DarkstoreMapper darkstoreMapper;

    @Override
    public Page<DarkstoreResponse> getAllDarkstores(Pageable pageable) {
        Page<DarkstoreEntity> darkstores = darkstoreRepository.readAllByActiveTrue(pageable);
        return darkstores.map(darkstoreMapper::toDarkstoreResponse);
    }

    @Override
    public DarkstoreResponse getDarkstore(UUID id) {
        DarkstoreEntity darkstoreEntity = getDarkstoreEntity(id);
        return darkstoreMapper.toDarkstoreResponse(darkstoreEntity);
    }

    @Override
    public DarkstoreResponse addDarkstore(DarkstoreRequest darkstoreRequest) {
        DarkstoreEntity darkstoreEntity = darkstoreMapper.toDarkstoreEntity(darkstoreRequest);
        darkstoreRepository.save(darkstoreEntity);
        return darkstoreMapper.toDarkstoreResponse(darkstoreEntity);
    }

    @Override
    public DarkstoreResponse updateDarkstore(UUID id, DarkstoreRequest darkstoreRequest) {
        DarkstoreEntity toUpdate = getDarkstoreEntity(id);
        toUpdate = darkstoreMapper.updateFromRequest(toUpdate, darkstoreRequest);
        toUpdate = darkstoreRepository.save(toUpdate);
        return darkstoreMapper.toDarkstoreResponse(toUpdate);
    }

    @Override
    public void deleteDarkstore(UUID id) {
        DarkstoreEntity toDelete = getDarkstoreEntity(id);
        toDelete.setActive(false);
        darkstoreRepository.save(toDelete);
    }

    private DarkstoreEntity getDarkstoreEntity(UUID id) {
        return darkstoreRepository.findById(id).orElseThrow(() ->
                new NoSuchElementException("There is no Darkstore with id: " + id));
    }
}
