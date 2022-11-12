package org.sniff.service;

import java.io.IOException;

public interface SplitService {

    void split(byte[] bytes) throws IOException;
}
