package com.fds.warehouseservice.service.darkstores;

import com.fds.warehouseservice.dto.storagebin.StorageBinResponse;
import com.fds.warehouseservice.dto.storagebin.StorageBinRequest;
import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.darkstores.StorageBinEntity;
import com.fds.warehouseservice.mapper.darkstores.StorageBinMapper;
import com.fds.warehouseservice.repository.darkstores.DarkstoreRepository;
import com.fds.warehouseservice.repository.darkstores.StorageBinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageBinServiceImpl implements StorageBinService {

    private final StorageBinRepository binRepository;
    private final DarkstoreRepository darkstoreRepository;
    private final StorageBinMapper storageBinMapper;

    @Override
    public Page<StorageBinResponse> getBinsByDarkstoreId(UUID darkstoreId, Pageable pageable) {
        Page<StorageBinEntity> bins = binRepository.readAllByDarkstoreIdAndActiveTrue(darkstoreId, pageable);
        return bins.map(storageBinMapper::toBinResponse);
    }

    @Override
    public StorageBinResponse getBinById(UUID binId, UUID darkstoreId) {
        StorageBinEntity storageBin = getStorageBin(binId, darkstoreId);
        return storageBinMapper.toBinResponse(storageBin);
    }

    @Override
    public StorageBinResponse addDarkstoreStorageBin(UUID darkstoreId, StorageBinRequest request) {
        DarkstoreEntity darkstore = darkstoreRepository.findById(darkstoreId).orElseThrow(() ->
                new NoSuchElementException("There is no Darkstore with id: " + darkstoreId));

        StorageBinEntity bin = storageBinMapper.toBinEntity(request);
        bin.setDarkstore(darkstore);

        bin = binRepository.save(bin);
        return storageBinMapper.toBinResponse(bin);
    }

    @Override
    public StorageBinResponse updateStorageBin(UUID binId, UUID darkstoreId, StorageBinRequest request) {
        StorageBinEntity bin = getStorageBin(binId, darkstoreId);

        bin = storageBinMapper.updateFromRequest(bin, request);
        bin = binRepository.save(bin);

        return storageBinMapper.toBinResponse(bin);
    }

    @Override
    public void deleteStorageBinById(UUID binId, UUID darkstoreId) {
        StorageBinEntity bin = getStorageBin(binId, darkstoreId);
        bin.setActive(false);
        binRepository.save(bin);
    }

    private StorageBinEntity getStorageBin(UUID binId, UUID darkstoreId) {
        return binRepository.findByIdAndDarkstoreIdAndActiveTrue(binId, darkstoreId).orElseThrow(() ->
                new NoSuchElementException("There is no storage bin with id: " + binId + "in specified darkstore"));
    }
}
