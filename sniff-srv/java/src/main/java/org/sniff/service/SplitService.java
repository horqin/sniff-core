package org.sniff.service;

import java.io.IOException;
import java.io.InputStream;

public interface SplitService {

    void split(InputStream inputStream) throws IOException;
}
