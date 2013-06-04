/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

import static client.model.Species.*;

/**
 * Class representing reference sequences of a certain build.
 *
 * @author bram
 */
public enum Assembly {

    HG19(Human),
    HG18(Human),
    MM10(Mouse),
    MM9(Mouse),
    TEST_REF(Test);
    private Species species;

    private Assembly(Species species) {
        this.species = species;
    }

    public String toFileName() {
        return name().toLowerCase();
    }

    @Override
    public String toString() {
        return species + " (" + name().toLowerCase() + ")";
    }
}
