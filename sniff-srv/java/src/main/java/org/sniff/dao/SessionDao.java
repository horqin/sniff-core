package org.sniff.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.sniff.entity.Session;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionDao extends BaseMapper<Session> {
}
