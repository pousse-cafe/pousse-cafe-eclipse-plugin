package poussecafe.eclipse.plugin.core;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import poussecafe.source.analysis.Name;

public class JavaNameResolver {

    public static String resolveSignature(IType type, String typeSignature) throws JavaModelException, IllegalArgumentException {
        var typeErasure = Signature.getTypeErasure(typeSignature);
        if(typeErasure.charAt(0) == Signature.C_UNRESOLVED) {
            var resolvedName = resolve(type, Signature.toString(typeErasure));
            if(resolvedName != null) {
                return resolvedName;
            } else {
                return Signature.toString(typeErasure);
            }
        } else {
            return Signature.toString(typeErasure);
        }
    }

    public static String resolve(IType type, String typeName) throws JavaModelException, IllegalArgumentException {
        var resolvedName = type.resolveType(typeName);
        if(resolvedName != null
                && resolvedName.length > 0) {
            return Signature.toQualifiedName(resolvedName[0]);
        } else {
            return null;
        }
    }

    public static String simpleName(String typeSignature) {
        var typeErasure = Signature.getTypeErasure(typeSignature);
        var nameString = Signature.toString(typeErasure);
        return new Name(nameString).simple();
    }

    private JavaNameResolver() {

    }
}
