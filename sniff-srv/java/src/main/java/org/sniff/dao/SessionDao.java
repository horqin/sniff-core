package org.sniff.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.sniff.entity.Session;
import org.springframework.stereotype.Service;

@Service
public interface SessionDao extends BaseMapper<Session> {
}
