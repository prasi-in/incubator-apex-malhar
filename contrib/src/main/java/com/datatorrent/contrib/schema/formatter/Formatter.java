/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.datatorrent.contrib.schema.formatter;

import org.apache.hadoop.classification.InterfaceStability;

import com.datatorrent.api.Context;
import com.datatorrent.api.Context.PortContext;
import com.datatorrent.api.DefaultInputPort;
import com.datatorrent.api.DefaultOutputPort;
import com.datatorrent.api.Operator.ActivationListener;
import com.datatorrent.api.annotation.InputPortFieldAnnotation;
import com.datatorrent.api.annotation.OutputPortFieldAnnotation;
import com.datatorrent.common.util.BaseOperator;
import com.datatorrent.contrib.converter.Converter;

/**
 * Abstract class that implements Converter interface. This is a schema enabled
 * Formatter <br>
 * Sub classes need to implement the convert method <br>
 * <b>Port Interface</b><br>
 * <b>in</b>: expects &lt;Object&gt; this is a schema enabled port<br>
 * <b>out</b>: emits &lt;OUTPUT&gt; <br>
 * <b>err</b>: emits &lt;Object&gt; error port that emits input tuple that could
 * not be converted<br>
 * <br>
 * 
 * @displayName Parser
 * @tags parser converter
 * @param <INPUT>
 * @since 3.2.0
 */
@InterfaceStability.Evolving
public abstract class Formatter<OUTPUT> extends BaseOperator implements Converter<Object, OUTPUT>,
    ActivationListener<Context>
{
  protected transient Class<?> clazz;

  @OutputPortFieldAnnotation
  public transient DefaultOutputPort<OUTPUT> out = new DefaultOutputPort<OUTPUT>();

  @OutputPortFieldAnnotation(optional = true)
  public transient DefaultOutputPort<Object> err = new DefaultOutputPort<Object>();

  @InputPortFieldAnnotation(schemaRequired = true)
  public transient DefaultInputPort<Object> in = new DefaultInputPort<Object>()
  {
    public void setup(PortContext context)
    {
      clazz = context.getValue(Context.PortContext.TUPLE_CLASS);
    }

    @Override
    public void process(Object inputTuple)
    {
      OUTPUT tuple = convert(inputTuple);
      if (tuple == null && err.isConnected()) {
        err.emit(inputTuple);
        return;
      }
      if (out.isConnected()) {
        out.emit(tuple);
      }
    }
  };

  /**
   * Get the class that needs to be formatted
   * 
   * @return Class<?>
   */
  public Class<?> getClazz()
  {
    return clazz;
  }

  /**
   * Set the class of tuple that needs to be formatted
   * 
   * @param clazz
   */
  public void setClazz(Class<?> clazz)
  {
    this.clazz = clazz;
  }
}
