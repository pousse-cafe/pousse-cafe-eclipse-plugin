package poussecafe.eclipse.plugin.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

@SuppressWarnings("serial")
public class BuilderState implements Serializable {

    public void serialize(IFile stateFile) throws IOException, CoreException {
        var bytes = new ByteArrayOutputStream();
        var oos = new ObjectOutputStream(bytes);
        oos.writeObject(sourceModelVisitorState);
        oos.writeObject(validationModelVisitorState);
        oos.close();

        var byteInputStream = new ByteArrayInputStream(bytes.toByteArray());
        if(stateFile.exists()) {
            stateFile.setContents(byteInputStream, IResource.FORCE, null);
        } else {
            stateFile.create(byteInputStream, false, null);
        }
    }

    public static BuilderState deserialize(IFile stateFile) throws Exception {
        var state = new BuilderState();
        var ois = new ObjectInputStream(stateFile.getContents());
        state.sourceModelVisitorState = (Serializable) ois.readObject();
        state.validationModelVisitorState = (Serializable) ois.readObject();
        ois.close();
        return state;
    }

    public void setSourceModelVisitorState(Serializable serializableState) {
        sourceModelVisitorState = serializableState;
    }

    private Serializable sourceModelVisitorState;

    public Serializable getSourceModelVisitorState() {
        return sourceModelVisitorState;
    }

    public void setValidationModelVisitorState(Serializable serializableState) {
        validationModelVisitorState = serializableState;
    }

    private Serializable validationModelVisitorState;

    public Serializable getValidationModelVisitorState() {
        return validationModelVisitorState;
    }
}
