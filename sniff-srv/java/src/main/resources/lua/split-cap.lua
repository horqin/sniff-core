if redis.call('SISMEMBER', 'done-session-entry', ARGV[1]) == 0 then
    local count = redis.call('ZCARD', 'session::' .. ARGV[1])
    if count < tonumber(KEYS[1]) then
        redis.call('ZADD', 'session::' .. ARGV[1], ARGV[2], ARGV[3])
        return count + 1
    end
end