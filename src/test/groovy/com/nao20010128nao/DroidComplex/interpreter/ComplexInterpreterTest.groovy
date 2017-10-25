package com.nao20010128nao.DroidComplex.interpreter

import org.apfloat.Apcomplex
import org.apfloat.Apfloat

/**
 * Created by lesmi on 17/10/26.
 */
class ComplexInterpreterTest extends GroovyTestCase {
    static{
        ComplexInterpreter.installMetaClass()
    }
    void testSimple(){
        def compiler=new GroovyShell()
        def parsed=compiler.parse('2+i3')
        parsed.binding=new ComplexInterpreter.ComplexNumberBinding()
        Apcomplex result=parsed.run()
        assert result.real()==new Apfloat(2)
        assert result.imag()==new Apfloat(3)
    }
}
