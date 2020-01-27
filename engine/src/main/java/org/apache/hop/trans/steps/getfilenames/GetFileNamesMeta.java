/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
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

package org.apache.hop.trans.steps.getfilenames;

import org.apache.commons.vfs2.FileObject;
import org.apache.hop.core.CheckResult;
import org.apache.hop.core.CheckResultInterface;
import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopStepException;
import org.apache.hop.core.exception.HopXMLException;
import org.apache.hop.core.fileinput.FileInputList;
import org.apache.hop.core.row.RowMetaInterface;
import org.apache.hop.core.row.ValueMetaInterface;
import org.apache.hop.core.row.value.ValueMetaBoolean;
import org.apache.hop.core.row.value.ValueMetaDate;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.VariableSpace;
import org.apache.hop.core.vfs.HopVFS;
import org.apache.hop.core.xml.XMLHandler;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metastore.api.IMetaStore;
import org.apache.hop.resource.ResourceDefinition;
import org.apache.hop.resource.ResourceEntry;
import org.apache.hop.resource.ResourceEntry.ResourceType;
import org.apache.hop.resource.ResourceNamingInterface;
import org.apache.hop.resource.ResourceReference;
import org.apache.hop.trans.Trans;
import org.apache.hop.trans.TransMeta;
import org.apache.hop.trans.step.BaseStepMeta;
import org.apache.hop.trans.step.StepDataInterface;
import org.apache.hop.trans.step.StepInterface;
import org.apache.hop.trans.step.StepMeta;
import org.apache.hop.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetFileNamesMeta extends BaseStepMeta implements StepMetaInterface {
  private static Class<?> PKG = GetFileNamesMeta.class; // for i18n purposes, needed by Translator2!!

  public static final String[] RequiredFilesDesc = new String[] {
    BaseMessages.getString( PKG, "System.Combo.No" ), BaseMessages.getString( PKG, "System.Combo.Yes" ) };
  public static final String[] RequiredFilesCode = new String[] { "N", "Y" };

  private static final String NO = "N";

  private static final String YES = "Y";

  /**
   * Array of filenames
   */
  private String[] fileName;

  /**
   * Wildcard or filemask (regular expression)
   */
  private String[] fileMask;

  /**
   * Wildcard or filemask to exclude (regular expression)
   */
  private String[] excludeFileMask;

  /**
   * Array of boolean values as string, indicating if a file is required.
   */
  private String[] fileRequired;

  /**
   * Array of boolean values as string, indicating if we need to fetch sub folders.
   */
  private String[] includeSubFolders;

  /**
   * Filter indicating file filter
   */
  private FileInputList.FileTypeFilter fileTypeFilter;

  /**
   * The name of the field in the output containing the filename
   */
  private String filenameField;

  /**
   * Flag indicating that a row number field should be included in the output
   */
  private boolean includeRowNumber;

  /**
   * The name of the field in the output containing the row number
   */
  private String rowNumberField;

  private String dynamicFilenameField;

  private String dynamicWildcardField;
  private String dynamicExcludeWildcardField;

  /**
   * file name from previous fields
   **/
  private boolean filefield;

  private boolean dynamicIncludeSubFolders;

  private boolean isaddresult;

  /**
   * The maximum number or lines to read
   */
  private long rowLimit;

  /**
   * Flag : do not fail if no file
   */
  private boolean doNotFailIfNoFile;

  public GetFileNamesMeta() {
    super(); // allocate BaseStepMeta
  }

  /**
   * @return the doNotFailIfNoFile flag
   */
  public boolean isdoNotFailIfNoFile() {
    return doNotFailIfNoFile;
  }

  /**
   * @param doNotFailIfNoFile the doNotFailIfNoFile to set
   */
  public void setdoNotFailIfNoFile( boolean doNotFailIfNoFile ) {
    this.doNotFailIfNoFile = doNotFailIfNoFile;
  }

  /**
   * @return Returns the filenameField.
   */
  public String getFilenameField() {
    return filenameField;
  }

  /**
   * @return Returns the rowNumberField.
   */
  public String getRowNumberField() {
    return rowNumberField;
  }

  /**
   * @param dynamicFilenameField The dynamic filename field to set.
   */
  public void setDynamicFilenameField( String dynamicFilenameField ) {
    this.dynamicFilenameField = dynamicFilenameField;
  }

  /**
   * @param dynamicWildcardField The dynamic wildcard field to set.
   */
  public void setDynamicWildcardField( String dynamicWildcardField ) {
    this.dynamicWildcardField = dynamicWildcardField;
  }

  /**
   * @param rowNumberField The rowNumberField to set.
   */
  public void setRowNumberField( String rowNumberField ) {
    this.rowNumberField = rowNumberField;
  }

  /**
   * @return Returns the dynamic filename field (from previous steps)
   */
  public String getDynamicFilenameField() {
    return dynamicFilenameField;
  }

  /**
   * @return Returns the dynamic wildcard field (from previous steps)
   */
  public String getDynamicWildcardField() {
    return dynamicWildcardField;
  }

  public String getDynamicExcludeWildcardField() {
    return this.dynamicExcludeWildcardField;
  }

  /**
   * @param dynamicExcludeWildcardField The dynamic excludeWildcard field to set.
   */
  public void setDynamicExcludeWildcardField( String dynamicExcludeWildcardField ) {
    this.dynamicExcludeWildcardField = dynamicExcludeWildcardField;
  }

  /**
   * @return Returns the includeRowNumber.
   */
  public boolean includeRowNumber() {
    return includeRowNumber;
  }

  /**
   * @return Returns the File field.
   */
  public boolean isFileField() {
    return filefield;
  }

  /**
   * @param filefield The filefield to set.
   */
  public void setFileField( boolean filefield ) {
    this.filefield = filefield;
  }

  public boolean isDynamicIncludeSubFolders() {
    return dynamicIncludeSubFolders;
  }

  public void setDynamicIncludeSubFolders( boolean dynamicIncludeSubFolders ) {
    this.dynamicIncludeSubFolders = dynamicIncludeSubFolders;
  }

  /**
   * @param includeRowNumber The includeRowNumber to set.
   */
  public void setIncludeRowNumber( boolean includeRowNumber ) {
    this.includeRowNumber = includeRowNumber;
  }

  /**
   * @param isaddresult The isaddresult to set.
   */
  public void setAddResultFile( boolean isaddresult ) {
    this.isaddresult = isaddresult;
  }

  /**
   * @return Returns isaddresult.
   */
  public boolean isAddResultFile() {
    return isaddresult;
  }

  /**
   * @return Returns the fileMask.
   */
  public String[] getFileMask() {
    return fileMask;
  }

  /**
   * @return Returns the fileRequired.
   */
  public String[] getFileRequired() {
    return fileRequired;
  }

  /**
   * @param fileMask The fileMask to set.
   */
  public void setFileMask( String[] fileMask ) {
    this.fileMask = fileMask;
  }

  /**
   * @param excludeFileMask The excludeFileMask to set.
   */
  public void setExcludeFileMask( String[] excludeFileMask ) {
    this.excludeFileMask = excludeFileMask;
  }

  /**
   * @return Returns the excludeFileMask.
   * Deprecated due to typo
   */
  @Deprecated
  public String[] getExludeFileMask() {
    return excludeFileMask;
  }

  /**
   * @return Returns the excludeFileMask.
   */
  public String[] getExcludeFileMask() {
    return excludeFileMask;
  }

  /**
   * @param fileRequiredin The fileRequired to set.
   */
  public void setFileRequired( String[] fileRequiredin ) {
    this.fileRequired = new String[ fileRequiredin.length ];
    for ( int i = 0; i < fileRequiredin.length; i++ ) {
      this.fileRequired[ i ] = getRequiredFilesCode( fileRequiredin[ i ] );
    }
  }

  /**
   * @return Returns the fileName.
   */
  public String[] getFileName() {
    return fileName;
  }

  /**
   * @param fileName The fileName to set.
   */
  public void setFileName( String[] fileName ) {
    this.fileName = fileName;
  }

  public String getRequiredFilesDesc( String tt ) {
    if ( tt == null ) {
      return RequiredFilesDesc[ 0 ];
    }
    if ( tt.equals( RequiredFilesCode[ 1 ] ) ) {
      return RequiredFilesDesc[ 1 ];
    } else {
      return RequiredFilesDesc[ 0 ];
    }
  }

  public void setIncludeSubFolders( String[] includeSubFoldersin ) {
    this.includeSubFolders = new String[ includeSubFoldersin.length ];
    for ( int i = 0; i < includeSubFoldersin.length; i++ ) {
      this.includeSubFolders[ i ] = getRequiredFilesCode( includeSubFoldersin[ i ] );
    }
  }

  public String getRequiredFilesCode( String tt ) {
    if ( tt == null ) {
      return RequiredFilesCode[ 0 ];
    }
    if ( tt.equals( RequiredFilesDesc[ 1 ] ) ) {
      return RequiredFilesCode[ 1 ];
    } else {
      return RequiredFilesCode[ 0 ];
    }
  }

  /**
   * @return Returns the rowLimit.
   */
  public long getRowLimit() {
    return rowLimit;
  }

  /**
   * @param rowLimit The rowLimit to set.
   */
  public void setRowLimit( long rowLimit ) {
    this.rowLimit = rowLimit;
  }

  @Deprecated
  public void setFilterFileType( int filtertypevalue ) {
    this.fileTypeFilter = FileInputList.FileTypeFilter.getByOrdinal( filtertypevalue );
  }

  public void setFilterFileType( FileInputList.FileTypeFilter filter ) {
    this.fileTypeFilter = filter;
  }

  public FileInputList.FileTypeFilter getFileTypeFilter() {
    return fileTypeFilter;
  }

  @Override
  public void loadXML( Node stepnode, IMetaStore metaStore ) throws HopXMLException {
    readData( stepnode );
  }

  @Override
  public Object clone() {
    GetFileNamesMeta retval = (GetFileNamesMeta) super.clone();

    int nrfiles = fileName.length;

    retval.allocate( nrfiles );

    System.arraycopy( fileName, 0, retval.fileName, 0, nrfiles );
    System.arraycopy( fileMask, 0, retval.fileMask, 0, nrfiles );
    System.arraycopy( excludeFileMask, 0, retval.excludeFileMask, 0, nrfiles );
    System.arraycopy( fileRequired, 0, retval.fileRequired, 0, nrfiles );
    System.arraycopy( includeSubFolders, 0, retval.includeSubFolders, 0, nrfiles );

    return retval;
  }

  public void allocate( int nrfiles ) {
    fileName = new String[ nrfiles ];
    fileMask = new String[ nrfiles ];
    excludeFileMask = new String[ nrfiles ];
    fileRequired = new String[ nrfiles ];
    includeSubFolders = new String[ nrfiles ];
  }

  @Override
  public void setDefault() {
    int nrfiles = 0;
    doNotFailIfNoFile = false;
    fileTypeFilter = FileInputList.FileTypeFilter.FILES_AND_FOLDERS;
    isaddresult = true;
    filefield = false;
    includeRowNumber = false;
    rowNumberField = "";
    dynamicFilenameField = "";
    dynamicWildcardField = "";
    dynamicIncludeSubFolders = false;
    dynamicExcludeWildcardField = "";

    allocate( nrfiles );

    for ( int i = 0; i < nrfiles; i++ ) {
      fileName[ i ] = "filename" + ( i + 1 );
      fileMask[ i ] = "";
      excludeFileMask[ i ] = "";
      fileRequired[ i ] = NO;
      includeSubFolders[ i ] = NO;
    }
  }

  @Override
  public void getFields( RowMetaInterface row, String name, RowMetaInterface[] info, StepMeta nextStep,
                         VariableSpace space, IMetaStore metaStore ) throws HopStepException {

    // the filename
    ValueMetaInterface filename = new ValueMetaString( "filename" );
    filename.setLength( 500 );
    filename.setPrecision( -1 );
    filename.setOrigin( name );
    row.addValueMeta( filename );

    // the short filename
    ValueMetaInterface short_filename = new ValueMetaString( "short_filename" );
    short_filename.setLength( 500 );
    short_filename.setPrecision( -1 );
    short_filename.setOrigin( name );
    row.addValueMeta( short_filename );

    // the path
    ValueMetaInterface path = new ValueMetaString( "path" );
    path.setLength( 500 );
    path.setPrecision( -1 );
    path.setOrigin( name );
    row.addValueMeta( path );

    // the type
    ValueMetaInterface type = new ValueMetaString( "type" );
    type.setLength( 500 );
    type.setPrecision( -1 );
    type.setOrigin( name );
    row.addValueMeta( type );

    // the exists
    ValueMetaInterface exists = new ValueMetaBoolean( "exists" );
    exists.setOrigin( name );
    row.addValueMeta( exists );

    // the ishidden
    ValueMetaInterface ishidden = new ValueMetaBoolean( "ishidden" );
    ishidden.setOrigin( name );
    row.addValueMeta( ishidden );

    // the isreadable
    ValueMetaInterface isreadable = new ValueMetaBoolean( "isreadable" );
    isreadable.setOrigin( name );
    row.addValueMeta( isreadable );

    // the iswriteable
    ValueMetaInterface iswriteable = new ValueMetaBoolean( "iswriteable" );
    iswriteable.setOrigin( name );
    row.addValueMeta( iswriteable );

    // the lastmodifiedtime
    ValueMetaInterface lastmodifiedtime = new ValueMetaDate( "lastmodifiedtime" );
    lastmodifiedtime.setOrigin( name );
    row.addValueMeta( lastmodifiedtime );

    // the size
    ValueMetaInterface size = new ValueMetaInteger( "size" );
    size.setOrigin( name );
    row.addValueMeta( size );

    // the extension
    ValueMetaInterface extension = new ValueMetaString( "extension" );
    extension.setOrigin( name );
    row.addValueMeta( extension );

    // the uri
    ValueMetaInterface uri = new ValueMetaString( "uri" );
    uri.setOrigin( name );
    row.addValueMeta( uri );

    // the rooturi
    ValueMetaInterface rooturi = new ValueMetaString( "rooturi" );
    rooturi.setOrigin( name );
    row.addValueMeta( rooturi );

    if ( includeRowNumber ) {
      ValueMetaInterface v = new ValueMetaInteger( space.environmentSubstitute( rowNumberField ) );
      v.setLength( ValueMetaInterface.DEFAULT_INTEGER_LENGTH, 0 );
      v.setOrigin( name );
      row.addValueMeta( v );
    }

  }

  @Override
  public String getXML() {
    StringBuilder retval = new StringBuilder( 300 );

    retval.append( "    <filter>" ).append( Const.CR );
    retval.append( "      " ).append( XMLHandler.addTagValue( "filterfiletype", fileTypeFilter.toString() ) );
    retval.append( "    </filter>" ).append( Const.CR );
    retval.append( "    " ).append( XMLHandler.addTagValue( "doNotFailIfNoFile", doNotFailIfNoFile ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "rownum", includeRowNumber ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "isaddresult", isaddresult ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "filefield", filefield ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "rownum_field", rowNumberField ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "filename_Field", dynamicFilenameField ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "wildcard_Field", dynamicWildcardField ) );
    retval
      .append( "    " ).append( XMLHandler.addTagValue( "exclude_wildcard_Field", dynamicExcludeWildcardField ) );
    retval.append( "    " ).append(
      XMLHandler.addTagValue( "dynamic_include_subfolders", dynamicIncludeSubFolders ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "limit", rowLimit ) );

    retval.append( "    <file>" ).append( Const.CR );

    for ( int i = 0; i < fileName.length; i++ ) {
      retval.append( "      " ).append( XMLHandler.addTagValue( "name", fileName[ i ] ) );
      retval.append( "      " ).append( XMLHandler.addTagValue( "filemask", fileMask[ i ] ) );
      retval.append( "      " ).append( XMLHandler.addTagValue( "exclude_filemask", excludeFileMask[ i ] ) );
      retval.append( "      " ).append( XMLHandler.addTagValue( "file_required", fileRequired[ i ] ) );
      retval.append( "      " ).append( XMLHandler.addTagValue( "include_subfolders", includeSubFolders[ i ] ) );
    }
    retval.append( "    </file>" ).append( Const.CR );

    return retval.toString();
  }

  private void readData( Node stepnode ) throws HopXMLException {
    try {
      Node filternode = XMLHandler.getSubNode( stepnode, "filter" );
      Node filterfiletypenode = XMLHandler.getSubNode( filternode, "filterfiletype" );
      fileTypeFilter = FileInputList.FileTypeFilter.getByName( XMLHandler.getNodeValue( filterfiletypenode ) );
      doNotFailIfNoFile = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "doNotFailIfNoFile" ) );
      includeRowNumber = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "rownum" ) );
      isaddresult = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "isaddresult" ) );
      filefield = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "filefield" ) );
      rowNumberField = XMLHandler.getTagValue( stepnode, "rownum_field" );
      dynamicFilenameField = XMLHandler.getTagValue( stepnode, "filename_Field" );
      dynamicWildcardField = XMLHandler.getTagValue( stepnode, "wildcard_Field" );
      dynamicExcludeWildcardField = XMLHandler.getTagValue( stepnode, "exclude_wildcard_Field" );
      dynamicIncludeSubFolders =
        "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "dynamic_include_subfolders" ) );

      // Is there a limit on the number of rows we process?
      rowLimit = Const.toLong( XMLHandler.getTagValue( stepnode, "limit" ), 0L );

      Node filenode = XMLHandler.getSubNode( stepnode, "file" );
      int nrfiles = XMLHandler.countNodes( filenode, "name" );

      allocate( nrfiles );

      for ( int i = 0; i < nrfiles; i++ ) {
        Node filenamenode = XMLHandler.getSubNodeByNr( filenode, "name", i );
        Node filemasknode = XMLHandler.getSubNodeByNr( filenode, "filemask", i );
        Node excludefilemasknode = XMLHandler.getSubNodeByNr( filenode, "exclude_filemask", i );
        Node fileRequirednode = XMLHandler.getSubNodeByNr( filenode, "file_required", i );
        Node includeSubFoldersnode = XMLHandler.getSubNodeByNr( filenode, "include_subfolders", i );
        fileName[ i ] = XMLHandler.getNodeValue( filenamenode );
        fileMask[ i ] = XMLHandler.getNodeValue( filemasknode );
        excludeFileMask[ i ] = XMLHandler.getNodeValue( excludefilemasknode );
        fileRequired[ i ] = XMLHandler.getNodeValue( fileRequirednode );
        includeSubFolders[ i ] = XMLHandler.getNodeValue( includeSubFoldersnode );
      }
    } catch ( Exception e ) {
      throw new HopXMLException( "Unable to load step info from XML", e );
    }
  }

  private boolean[] includeSubFolderBoolean() {
    int len = fileName.length;
    boolean[] includeSubFolderBoolean = new boolean[ len ];
    for ( int i = 0; i < len; i++ ) {
      includeSubFolderBoolean[ i ] = YES.equalsIgnoreCase( includeSubFolders[ i ] );
    }
    return includeSubFolderBoolean;
  }

  public String[] getIncludeSubFolders() {
    return includeSubFolders;
  }

  private FileInputList.FileTypeFilter[] buildFileTypeFiltersArray( String[] fileName ) {
    FileInputList.FileTypeFilter[] filters = new FileInputList.FileTypeFilter[ fileName.length ];
    for ( int i = 0; i < fileName.length; i++ ) {
      filters[ i ] = getFileTypeFilter();
    }
    return filters;
  }

  public String[] getFilePaths( VariableSpace space ) {
    return FileInputList.createFilePathList(
      space, fileName, fileMask, excludeFileMask, fileRequired, includeSubFolderBoolean(),
      buildFileTypeFiltersArray( fileName ) );
  }

  public FileInputList getFileList( VariableSpace space ) {
    return FileInputList.createFileList(
      space, fileName, fileMask, excludeFileMask, fileRequired, includeSubFolderBoolean(),
      buildFileTypeFiltersArray( fileName ) );
  }

  public FileInputList getDynamicFileList( VariableSpace space, String[] filename, String[] filemask,
                                           String[] excludefilemask, String[] filerequired, boolean[] includesubfolders ) {
    return FileInputList.createFileList(
      space, filename, filemask, excludefilemask, filerequired, includesubfolders,
      buildFileTypeFiltersArray( filename ) );
  }

  @Override
  public void check( List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta,
                     RowMetaInterface prev, String[] input, String[] output, RowMetaInterface info, VariableSpace space,
                     IMetaStore metaStore ) {
    CheckResult cr;

    // See if we get input...
    if ( filefield ) {
      if ( input.length > 0 ) {
        cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(
            PKG, "GetFileNamesMeta.CheckResult.InputOk" ), stepMeta );
      } else {
        cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(
            PKG, "GetFileNamesMeta.CheckResult.InputErrorKo" ), stepMeta );
      }
      remarks.add( cr );

      if ( Utils.isEmpty( dynamicFilenameField ) ) {
        cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(
            PKG, "GetFileNamesMeta.CheckResult.FolderFieldnameMissing" ), stepMeta );
      } else {
        cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(
            PKG, "GetFileNamesMeta.CheckResult.FolderFieldnameOk" ), stepMeta );
      }
      remarks.add( cr );

    } else {

      if ( input.length > 0 ) {
        cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(
            PKG, "GetFileNamesMeta.CheckResult.NoInputError" ), stepMeta );
      } else {
        cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(
            PKG, "GetFileNamesMeta.CheckResult.NoInputOk" ), stepMeta );
      }

      remarks.add( cr );

      // check specified file names
      FileInputList fileList = getFileList( transMeta );
      if ( fileList.nrOfFiles() == 0 ) {
        cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(
            PKG, "GetFileNamesMeta.CheckResult.ExpectedFilesError" ), stepMeta );
      } else {
        cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(
            PKG, "GetFileNamesMeta.CheckResult.ExpectedFilesOk", "" + fileList.nrOfFiles() ), stepMeta );
      }
      remarks.add( cr );
    }
  }

  @Override
  public List<ResourceReference> getResourceDependencies( TransMeta transMeta, StepMeta stepInfo ) {
    List<ResourceReference> references = new ArrayList<ResourceReference>( 5 );
    ResourceReference reference = new ResourceReference( stepInfo );
    references.add( reference );

    String[] files = getFilePaths( transMeta );
    if ( files != null ) {
      for ( int i = 0; i < files.length; i++ ) {
        reference.getEntries().add( new ResourceEntry( files[ i ], ResourceType.FILE ) );
      }
    }
    return references;
  }

  @Override
  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr,
                                TransMeta transMeta, Trans trans ) {
    return new GetFileNames( stepMeta, stepDataInterface, cnr, transMeta, trans );
  }

  @Override
  public StepDataInterface getStepData() {
    return new GetFileNamesData();
  }

  /**
   * @param space                   the variable space to use
   * @param definitions
   * @param resourceNamingInterface
   * @param repository              The repository to optionally load other resources from (to be converted to XML)
   * @param metaStore               the metaStore in which non-kettle metadata could reside.
   * @return the filename of the exported resource
   */
  @Override
  public String exportResources( VariableSpace space, Map<String, ResourceDefinition> definitions,
                                 ResourceNamingInterface resourceNamingInterface, IMetaStore metaStore ) throws HopException {
    try {
      // The object that we're modifying here is a copy of the original!
      // So let's change the filename from relative to absolute by grabbing the file object...
      // In case the name of the file comes from previous steps, forget about this!
      //
      if ( !filefield ) {

        // Replace the filename ONLY (folder or filename)
        //
        for ( int i = 0; i < fileName.length; i++ ) {
          FileObject fileObject = HopVFS.getFileObject( space.environmentSubstitute( fileName[ i ] ), space );
          fileName[ i ] = resourceNamingInterface.nameResource( fileObject, space, Utils.isEmpty( fileMask[ i ] ) );
        }
      }
      return null;
    } catch ( Exception e ) {
      throw new HopException( e );
    }
  }

}