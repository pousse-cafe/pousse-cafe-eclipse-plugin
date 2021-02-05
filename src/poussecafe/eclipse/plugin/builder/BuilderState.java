package poussecafe.eclipse.plugin.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

@SuppressWarnings("serial")
public class BuilderState implements Serializable {

    public void serialize(IFile stateFile) throws IOException, CoreException {
        var bytes = new ByteArrayOutputStream();
        var oos = new ObjectOutputStream(bytes);
        oos.writeObject(files);
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

    @SuppressWarnings("unchecked")
    public static BuilderState deserialize(IFile stateFile) throws Exception {
        var state = new BuilderState();
        var ois = new ObjectInputStream(stateFile.getContents());
        state.files = (Set<String>) ois.readObject();
        state.sourceModelVisitorState = (Serializable) ois.readObject();
        state.validationModelVisitorState = (Serializable) ois.readObject();
        ois.close();
        return state;
    }

    public void addFiles(Map<String, IFile> filesMap) {
        files.clear();
        files.addAll(filesMap.keySet());
    }

    private Set<String> files = new HashSet<>();

    public Set<String> files() {
        return Collections.unmodifiableSet(files);
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
