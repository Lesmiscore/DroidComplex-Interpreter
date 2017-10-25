package com.nao20010128nao.DroidComplex.interpreter

import android.content.Context
import me.champeau.groovydroid.GrooidShell
import org.apfloat.Apcomplex
import org.apfloat.ApcomplexMath
import org.apfloat.Apfloat

import java.util.regex.Pattern

public class ComplexInterpreter {
    static final String REGEX_NUMBER="([0-9]*\\.[0-9]+|[0-9]+)"

    private static ComplexInterpreter instance
    private static boolean metaClassInstalled=false

    static void install(Context app){
        if(instance){
            return
        }
        instance=new ComplexInterpreter(app.cacheDir,app.classLoader)
        installMetaClass()
    }

    static void installMetaClass(){
        if(metaClassInstalled){
            return
        }
        /* Set up MetaClass to interpret */
        Number.metaClass.plus={Number a->
            convert(delegate).add(convert(a))
        }
        Number.metaClass.minus={Number a->
            convert(delegate).subtract(convert(a))
        }
        Number.metaClass.multiply={Number a->
            ApcomplexMath.product(convert(delegate),convert(a))
        }
        Number.metaClass.div={Number a->
            convert(delegate).divide(convert(a))
        }
        Number.metaClass.positive={->
            convert(delegate)
        }
        Number.metaClass.negative={->
            convert(delegate).negate()
        }
        Apcomplex.I.metaClass.call={Number a->
            convert(delegate)*convert(a)
        }
        metaClassInstalled=true
    }

    private static Apcomplex convert(a){
        if(a instanceof Apcomplex){
            /* No needs to be converted */
            return a
        }else if(a instanceof Number){
            /*
             * The Number must not be an Apcomplex,
             * so this will be a real number
             */
            return new Apfloat(a)
        }else if(a instanceof CharSequence){
            def inStr="$a"
            /* Try with Apcomplex */
            try{
                return new Apcomplex(inStr)
            }catch (Throwable e){

            }
            /*
             * Statically parsing:
             * Input should have the following format;
             * i, j
             * [real]+i[imag], [real]+j[imag]
             * [real]+[imag]i, [real]+[imag]j
             * i[imag], j[imag]
             * [imag]i, [imag]j
             * */
            def first="[+-]?[ij]"
            def second="$REGEX_NUMBER[+-][ij]$REGEX_NUMBER"
            def third="$REGEX_NUMBER[+-]$REGEX_NUMBER[ij]"
            def forth="[+-]?[ij]$REGEX_NUMBER"
            def fifth="[+-]?$REGEX_NUMBER[ij]"
            if(inStr.matches(first)){
                return Apcomplex.I
            }else if(inStr.matches(second)||inStr.matches(third)){
                def (re,im)=inStr.findAll(Pattern.compile(REGEX_NUMBER)).collect{new Apfloat(it)}
                return new Apcomplex(re,im)
            }else if(inStr.matches(forth)||inStr.matches(fifth)){
                def im=inStr.findAll(Pattern.compile(REGEX_NUMBER)).collect{new Apfloat(it)}
                return new Apcomplex(Apfloat.ZERO,im)
            }
        }
        throw new NumberFormatException("We can't understand what you request: $a")
    }

    final File cache
    final GrooidShell compiler

    private ComplexInterpreter(File cache,ClassLoader loader){
        this.cache=cache
        this.compiler=new GrooidShell(cache,loader)
    }

    public Compilation compile(String script){
        def result=new Compilation()
        try{
            def compileResult=compiler.evaluate(script){cfg->
                cfg.scriptBaseClass="$DroidComplexScript.name"
            }
            result.success=true
            result.script=compileResult.script
        }catch (Throwable e){
            result.success=false
            result.error=e
        }
        return result
    }


    class Compilation{
        boolean success
        Script script
        Throwable error
    }

    static abstract class DroidComplexScript extends Script{
        public DroidComplexScript(){
            super(new ComplexNumberBinding())
        }
    }

    static class ComplexNumberBinding extends Binding{
        @Override
        Object getVariable(String name) {
            if(name.matches('^[ij]$')){
                return Apcomplex.I
            }else if(name.matches("^[ij]$REGEX_NUMBER\$")||name.matches("^$REGEX_NUMBER[ij]\$")){
                return new Apcomplex(Apfloat.ZERO,new Apfloat(name.findAll(REGEX_NUMBER).first()))
            }else if(name.matches('^[ij]')&&super.getVariable(name) instanceof Number){
                return super.getVariable(name.substring(1))*Apcomplex.I
            }else if(name.matches('[ij]$')&&super.getVariable(name) instanceof Number){
                return super.getVariable(name.substring(0,name.length()-1))*Apcomplex.I
            }
            return super.getVariable(name)
        }

        @Override
        void setVariable(String name, Object value) {
            if(name.matches('^[ij]')){
                throw new Error('Cannot set to any field starts with "i" or "j".')
            }
            super.setVariable(name, value)
        }
    }
}
