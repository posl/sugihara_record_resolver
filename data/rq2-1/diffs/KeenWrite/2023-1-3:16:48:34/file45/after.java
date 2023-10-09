package com.keenwrite.typesetting.installer.panes;

import com.keenwrite.io.CommandNotFoundException;
import com.keenwrite.typesetting.containerization.ContainerManager;
import com.keenwrite.typesetting.containerization.StreamProcessor;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import org.apache.commons.lang3.function.FailableBiConsumer;
import org.controlsfx.dialog.Wizard;

import static com.keenwrite.Messages.get;
import static com.keenwrite.io.StreamGobbler.gobble;

/**
 * Responsible for showing the output from running commands against a container
 * manager. There are a few installation steps that run different commands
 * against the installer, which are platform-specific and cannot be merged.
 * Common functionality between them is codified in this class.
 */
public abstract class ManagerOutputPane extends InstallerPane {
  private final String PROP_EXECUTOR = getClass().getCanonicalName();

  private final String mCorrectKey;
  private final String mMissingKey;
  private final FailableBiConsumer
    <ContainerManager, StreamProcessor, CommandNotFoundException> mFc;
  private final ContainerManager mContainer;
  private final TextArea mTextArea;

  public ManagerOutputPane(
    final String correctKey,
    final String missingKey,
    final FailableBiConsumer
      <ContainerManager, StreamProcessor, CommandNotFoundException> fc,
    final int cols
  ) {
    mFc = fc;
    mCorrectKey = correctKey;
    mMissingKey = missingKey;
    mTextArea = textArea( 5, cols );
    mContainer = createContainer();

    final var borderPane = new BorderPane();
    final var titledPane = titledPane( "Output", mTextArea );

    borderPane.setBottom( titledPane );
    setContent( borderPane );
  }

  @Override
  public void onEnteringPage( final Wizard wizard ) {
    disableNext( true );

    try {
      final var properties = wizard.getProperties();
      final var thread = properties.get( PROP_EXECUTOR );

      if( thread instanceof Thread executor && executor.isAlive() ) {
        return;
      }

      final Task<Void> task = createTask( () -> {
        mFc.accept(
          mContainer,
          input -> gobble( input, line -> append( mTextArea, line ) )
        );
        properties.remove( thread );
        return null;
      } );

      task.setOnSucceeded( event -> {
        append( mTextArea, get( mCorrectKey ) );
        properties.remove( thread );
        disableNext( false );
      } );
      task.setOnFailed( event -> append( mTextArea, get( mMissingKey ) ) );
      task.setOnCancelled( event -> append( mTextArea, get( mMissingKey ) ) );

      final var executor = createThread( task );
      properties.put( PROP_EXECUTOR, executor );
      executor.start();
    } catch( final Exception e ) {
      throw new RuntimeException( e );
    }
  }
}
