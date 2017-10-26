package com.nao20010128nao.DroidComplex.interpreter

import junit.framework.TestCase
import org.apfloat.Apcomplex
import org.apfloat.Apfloat

/**
 * Created by lesmi on 17/10/26.
 */
class ComplexInterpreterTest extends TestCase {
    def interpreter=ComplexInterpreter.instance
    static{
        ComplexInterpreter.install(null)
    }
    ComplexInterpreter.Compilation compile(String s){
        interpreter.compile(s)
    }

    void testSimple(){
        def script=compile('2+i3')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==new Apfloat(2)
        assert result.imag()==new Apfloat(3)
    }
}
