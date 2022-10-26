package rm4j.compiler.resolution;

import rm4j.compiler.tree.TypeParameterTree;

public record TypeAssignRule(TypeParameterTree typeParameter, Type assigned){}
