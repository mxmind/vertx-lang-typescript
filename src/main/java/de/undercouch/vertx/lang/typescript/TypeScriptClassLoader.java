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

package de.undercouch.vertx.lang.typescript;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import de.undercouch.vertx.lang.typescript.cache.Cache;
import de.undercouch.vertx.lang.typescript.compiler.Source;
import de.undercouch.vertx.lang.typescript.compiler.SourceFactory;
import de.undercouch.vertx.lang.typescript.compiler.TypeScriptCompiler;

/**
 * A special class loader that automatically compiles loaded TypeScript
 * sources to JavaScript.
 * @author Michel Kraemer
 */
public class TypeScriptClassLoader extends ClassLoader implements SourceFactory {
  /**
   * A cache for already loaded source files
   */
  private Map<String, Source> sourceCache = new HashMap<>();
  
  /**
   * A cache for already compiled sources
   */
  private final Cache codeCache;
  
  /**
   * A TypeScript compiler
   */
  private final TypeScriptCompiler compiler;
  
  /**
   * Creates a new class loader
   * @param parent the parent class loader
   * @param compiler a TypeScript compiler
   * @param codeCache a cache for already compiled sources
   */
  public TypeScriptClassLoader(ClassLoader parent, TypeScriptCompiler compiler, Cache codeCache) {
    super(parent);
    this.compiler = compiler;
    this.codeCache = codeCache;
  }
  
  @Override
  public InputStream getResourceAsStream(String name) {
    try {
      // load and compile TypeScript sources
      String lowerName = name.toLowerCase();
      if (lowerName.endsWith(".ts")) {
        return load(name);
      } else if (lowerName.endsWith(".ts.js")) {
        return load(name.substring(0, name.length() - 3));
      }
      
      // try to load other files directly
      InputStream r = super.getResourceAsStream(name);
      if (r == null && lowerName.endsWith(".js")) {
        // try to load .ts file instead
        return load(name.substring(0, name.length() - 3) + ".ts");
      }
      return r;
    } catch (IOException e) {
      // according to the interface
      return null;
    }
  }
  
  @Override
  public Source getSource(String name, String baseFilename) throws IOException {
    if (baseFilename != null && (name.startsWith("./") || name.startsWith("../"))) {
      // resolve relative path
      Source baseSource = getSource(baseFilename, null);
      if (baseSource != null) {
        name = baseSource.getURI().resolve(name).normalize().toString();
      }
    }
    
    Source result = sourceCache.get(name);
    if (result == null) {
      // check if we've got a URL
      URL u;
      if (name.matches("^[a-z]+:/.*")) {
        try {
          u = new URL(name);
        } catch (MalformedURLException e) {
          // no URL, might be a Windows path instead
          u = null;
        }
      } else {
        u = null;
      }
      
      // search class path
      if (u == null) {
        u = getParent().getResource(name);
      }
      if (u != null) {
        result = Source.fromURL(u, StandardCharsets.UTF_8);
      }
      
      // search file system (will throw if the file could not be found)
      if (result == null) {
        result = Source.fromFile(new File(name), StandardCharsets.UTF_8);
      }
      
      // at this point 'result' should never be null
      assert result != null;
      sourceCache.put(name, result);
    }
    
    return result;
  }
  
  /**
   * Loads and compiles a file with the given name
   * @param name the file name
   * @return an input stream delivering the 
   * @throws IOException
   */
  private InputStream load(String name) throws IOException {
    // load file from class path or from file system
    Source src = getSource(name, null);
    
    // check if we have compiled the file before
    String code = codeCache.get(src);
    if (code == null) {
      // compile it now
      code = compiler.compile(name, this);
      codeCache.put(src, code);
    }
    
    return new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
  }
}
