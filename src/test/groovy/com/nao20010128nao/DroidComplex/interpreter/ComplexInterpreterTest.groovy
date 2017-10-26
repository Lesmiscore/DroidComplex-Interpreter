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

    void testSpecialValue(){
        def script=compile('i')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==Apfloat.ZERO
        assert result.imag()==Apfloat.ONE
    }
    void testSpecialValue2(){
        def script=compile('j')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==Apfloat.ZERO
        assert result.imag()==Apfloat.ONE
    }
    void testSimple(){
        def script=compile('2+i3')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==new Apfloat(2)
        assert result.imag()==new Apfloat(3)
    }
    void testAdd(){
        def script=compile('(2+i3)+(j6+5)')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==new Apfloat(7)
        assert result.imag()==new Apfloat(9)
    }
    void testSubtract(){
        def script=compile('(2+i3)-(j6+5)')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==new Apfloat(-3)
        assert result.imag()==new Apfloat(-3)
    }
    void testMultiply(){
        def script=compile('(2+i3)*(3+i8)')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==new Apfloat(-18)
        assert result.imag()==new Apfloat(25)
    }
    void testDivision(){
        def script=compile('(6+i9)/(2+i3)')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==new Apfloat(3)
        assert result.imag()==Apfloat.ZERO
    }
    void testMultiplyAsMethodCall(){
        def script=compile('(2+i3)(4+i5)')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==new Apfloat(-7)
        assert result.imag()==new Apfloat(22)
    }
    void testMultiplyAsMethodCall2(){
        def script=compile('i(2+i3)')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==new Apfloat(-3)
        assert result.imag()==new Apfloat(2)
    }
}
