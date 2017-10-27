package com.nao20010128nao.DroidComplex.interpreter

import groovy.transform.Memoized
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
    @Memoized
    static Apfloat apfloat(Number a){
        new Apfloat(a)
    }
    ComplexInterpreter.Compilation compile(String s){
        interpreter.compile(s)
    }

    void testSpecialValue(){
        def script=compile('i')
        if(script.error)throw script.error
        Apcomplex result=script.script.run()
        assert result.real()==Apfloat.ZERO
        assert result.imag()==Apfloat.ONE
    }
    void testSpecialValue2(){
        def script=compile('j')
        if(script.error)throw script.error
        Apcomplex result=script.script.run()
        assert result.real()==Apfloat.ZERO
        assert result.imag()==Apfloat.ONE
    }
    void testSimple(){
        def script=compile('2+i3')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==apfloat(2)
        assert result.imag()==apfloat(3)
    }
    void testAdd(){
        def script=compile('(2+i3)+(j6+5)')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==apfloat(7)
        assert result.imag()==apfloat(9)
    }
    void testSubtract(){
        def script=compile('(2+i3)-(j6+5)')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==apfloat(-3)
        assert result.imag()==apfloat(-3)
    }
    void testMultiply(){
        def script=compile('(2+i3)*(3+i8)')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==apfloat(-18)
        assert result.imag()==apfloat(25)
    }
    void testDivision(){
        def script=compile('(6+i9)/(2+i3)')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==apfloat(3)
        assert result.imag()==Apfloat.ZERO
    }
    void testMultiplyAsMethodCall(){
        def script=compile('(2+i3)(4+i5)')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==apfloat(-7)
        assert result.imag()==apfloat(22)
    }
    void testMultiplyAsMethodCall2(){
        def script=compile('i(2+i3)')
        assert script.success
        Apcomplex result=script.script.run()
        assert result.real()==apfloat(-3)
        assert result.imag()==apfloat(2)
    }
    void testConvertFromStringFirst1(){
        Apcomplex result=ComplexInterpreter.convert('i')
        assert result.real()==Apfloat.ZERO
        assert result.imag()==Apfloat.ONE
    }
    void testConvertFromStringFirst2(){
        Apcomplex result=ComplexInterpreter.convert('+i')
        assert result.real()==Apfloat.ZERO
        assert result.imag()==Apfloat.ONE
    }
    void testConvertFromStringFirst3(){
        Apcomplex result=ComplexInterpreter.convert('-i')
        assert result.real()==Apfloat.ZERO
        assert result.imag()==-Apfloat.ONE
    }
    void testConvertFromStringSecond1(){
        Apcomplex result=ComplexInterpreter.convert('11+i13')
        assert result.real()==apfloat(11)
        assert result.imag()==apfloat(13)
    }
    void testConvertFromStringSecond2(){
        Apcomplex result=ComplexInterpreter.convert('+11+i13')
        assert result.real()==apfloat(11)
        assert result.imag()==apfloat(13)
    }
    void testConvertFromStringSecond3(){
        Apcomplex result=ComplexInterpreter.convert('11-i13')
        assert result.real()==apfloat(11)
        assert result.imag()==apfloat(-13)
    }
    void testConvertFromStringSecond4(){
        Apcomplex result=ComplexInterpreter.convert('+11-i13')
        assert result.real()==apfloat(11)
        assert result.imag()==apfloat(-13)
    }
    void testConvertFromStringSecond5(){
        Apcomplex result=ComplexInterpreter.convert('-11+i13')
        assert result.real()==apfloat(-11)
        assert result.imag()==apfloat(13)
    }
    void testConvertFromStringSecond6(){
        Apcomplex result=ComplexInterpreter.convert('-11-i13')
        assert result.real()==apfloat(-11)
        assert result.imag()==apfloat(-13)
    }
}
