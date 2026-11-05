package com.zhanhe.workforce.service;

import com.zhanhe.workforce.mapper.NationMapper;
import com.zhanhe.workforce.model.Nation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NationService {
    @Autowired
    NationMapper nationMapper;
    public List<Nation> getAllNations() {
        return nationMapper.getAllNations();
    }
}
