// Copyright 2015 Michel Kraemer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.undercouch.vertx.lang.typescript.compiler;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import de.undercouch.vertx.lang.typescript.TypeScriptClassLoader;
import de.undercouch.vertx.lang.typescript.cache.NoopCache;

/**
 * Common tests for all compilers
 * @author Michel Kraemer
 */
public abstract class CompilerTestBase {
  /**
   * @return the actual compiler to test
   */
  abstract protected TypeScriptCompiler getCompiler();
  
  /**
   * Compiles a very simple script
   * @throws Exception if something goes wrong
   */
  @Test
  public void simpleScript() throws Exception {
    TypeScriptClassLoader cl = new TypeScriptClassLoader(this.getClass().getClassLoader(),
        null, new NoopCache());
    String code = getCompiler().compile("test.ts", new SourceFactory() {
      @Override
      public Source getSource(String filename, String baseFilename) throws IOException {
        if (filename.equals("test.ts")) {
          try {
            return new Source(new URI(filename), "var i: number = 5;");
          } catch (URISyntaxException e) {
            throw new IOException("Illegal filename", e);
          }
        }
        return cl.getSource(filename, baseFilename);
      }
    });
    assertEquals("var i = 5;", code.trim());
  }
}
