/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.apache.hop.pipeline.transforms.jsoninput;

import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopXmlException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.xml.XmlHandler;
import org.apache.hop.metastore.api.IMetaStore;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by bmorrise on 3/22/16.
 */
@RunWith( MockitoJUnitRunner.class )
public class JsonInputMetaTest {
  public static final String DATA = "data";
  public static final String NAME = "name";
  private static final Pattern CLEAN_NODES = Pattern.compile( "(<transform>)[\\r|\\n]+|</transform>" );

  JsonInputMeta jsonInputMeta;

  @Mock
  IRowMeta rowMeta;

  @Mock
  IRowMeta rowMetaInterfaceItem;

  @Mock
  TransformMeta nextStep;

  @Mock
  IVariables space;

  @Mock
  IMetaStore metaStore;

  @Mock
  JsonInputMeta.InputFiles inputFiles;

  @Mock
  JsonInputField inputField;

  @Before
  public void setup() {
    jsonInputMeta = new JsonInputMeta();
    jsonInputMeta.setInputFiles( inputFiles );
    jsonInputMeta.setInputFields( new JsonInputField[] { inputField } );
  }

  @Test
  public void getFieldsRemoveSourceField() throws Exception {
    IRowMeta[] info = new IRowMeta[ 1 ];
    info[ 0 ] = rowMetaInterfaceItem;

    jsonInputMeta.setRemoveSourceField( true );
    jsonInputMeta.setFieldValue( DATA );
    jsonInputMeta.setInFields( true );

    when( rowMeta.indexOfValue( DATA ) ).thenReturn( 0 );

    jsonInputMeta.getFields( rowMeta, NAME, info, nextStep, space, metaStore );

    verify( rowMeta ).removeValueMeta( 0 );
  }

  @Test
  public void testGetXmlOfDefaultMeta_defaultPathLeafToNull_Y() throws Exception {
    jsonInputMeta = new JsonInputMeta();
    jsonInputMeta.setDefault();
    String xml = jsonInputMeta.getXml();
    assertEquals( expectedMeta( "/transform_default.xml" ), xml );
  }

  @Test
  public void testGetXmlOfMeta_defaultPathLeafToNull_N() throws Exception {
    jsonInputMeta = new JsonInputMeta();
    jsonInputMeta.setDefault();
    jsonInputMeta.setDefaultPathLeafToNull( false );
    String xml = jsonInputMeta.getXml();
    assertEquals( expectedMeta( "/transform_defaultPathLeafToNull_N.xml" ), xml );
  }

  // Loading step meta from the step xml where DefaultPathLeafToNull=N
  @Test
  public void testMetaLoad_DefaultPathLeafToNull_Is_N() throws HopXmlException {
    jsonInputMeta = new JsonInputMeta();
    jsonInputMeta.loadXml( loadTransformFile( "/transform_defaultPathLeafToNull_N.xml" ), metaStore );
    assertEquals( "Option.DEFAULT_PATH_LEAF_TO_NULL ", false, jsonInputMeta.isDefaultPathLeafToNull() );
  }

  // Loading step meta from default step xml. In this case DefaultPathLeafToNull=Y in xml.
  @Test
  public void testDefaultMetaLoad_DefaultPathLeafToNull_Is_Y() throws HopXmlException {
    jsonInputMeta = new JsonInputMeta();
    jsonInputMeta.loadXml( loadTransformFile( "/transform_default.xml" ), metaStore );
    assertEquals( "Option.DEFAULT_PATH_LEAF_TO_NULL ", true, jsonInputMeta.isDefaultPathLeafToNull() );
  }

  // Loading step meta from the step xml that was created before PDI-17060 fix. In this case xml contains no
  // DefaultPathLeafToNull node at all.
  // For backward compatibility in this case we think that the option is set to default value - Y.
  @Test
  public void testMetaLoadAsDefault_NoDefaultPathLeafToNull_In_Xml() throws HopXmlException {
    jsonInputMeta = new JsonInputMeta();
    jsonInputMeta.loadXml( loadTransformFile( "/transform_no_defaultPathLeafToNull_node.xml" ), metaStore );
    assertEquals( "Option.DEFAULT_PATH_LEAF_TO_NULL ", true, jsonInputMeta.isDefaultPathLeafToNull() );
  }

  private Node loadTransformFile( String transformFilename ) throws HopXmlException {
    Document document = XmlHandler.loadXmlFile( this.getClass().getResourceAsStream( transformFilename ) );
    Node stepNode = document.getDocumentElement();
    return stepNode;
  }

  private String expectedMeta( String step ) throws Exception {
    try ( BufferedReader reader = new BufferedReader( new InputStreamReader( this.getClass().getResourceAsStream( step ) ) ) ) {
      String xml = reader.lines().collect( Collectors.joining( Const.CR ) );
      xml = CLEAN_NODES.matcher( xml ).replaceAll( "" );
      return xml;
    }
  }
}
