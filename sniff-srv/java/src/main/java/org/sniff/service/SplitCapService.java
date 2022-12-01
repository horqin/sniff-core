package org.sniff.service;

import java.io.IOException;
import java.io.InputStream;

public interface SplitCapService {

    void splitCap(InputStream inputStream) throws IOException;
}
