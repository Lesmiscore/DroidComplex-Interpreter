package com.nao20010128nao.DroidComplex.interpreter

import android.content.Context
import dalvik.system.BaseDexClassLoader
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import me.champeau.groovydroid.GrooidShell
import org.apfloat.Apcomplex
import org.apfloat.ApcomplexMath
import org.apfloat.Apfloat
import org.codehaus.groovy.control.CompilerConfiguration

import java.util.regex.Pattern

public class ComplexInterpreter {
    static final String REGEX_NUMBER="([0-9]*\\.[0-9]+|[0-9]+)"
    static final String REGEX_SIGN="([+-])"
    static final String REGEX_SIGN_OPTIONAL="([+-]?)"

    private static ComplexInterpreter instance0
    private static long precision0
    private static boolean metaClassInstalled=false

    static void install(Context app,long precision){
        if(instance0){
            return
        }
        instance0=new ComplexInterpreter(app?.cacheDir,app?.classLoader)
        precision0=precision
        installMetaClass()
    }

    static void installMetaClass(){
        if(metaClassInstalled){
            return
        }
        /* Set up MetaClass to interpret */

        /* Number <-> Number */
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
        Number.metaClass.power={Number a->
            ApcomplexMath.pow(convert(delegate),convert(a))
        }
        Number.metaClass.call={Number a->
            convert(delegate)*convert(a)
        }

        /* Number <-> String */
        Number.metaClass.plus={String a->
            convert(delegate).add(convert(a))
        }
        Number.metaClass.minus={String a->
            convert(delegate).subtract(convert(a))
        }
        Number.metaClass.multiply={String a->
            ApcomplexMath.product(convert(delegate),convert(a))
        }
        Number.metaClass.div={String a->
            convert(delegate).divide(convert(a))
        }

        metaClassInstalled=true
    }

    @CompileStatic
    static ComplexInterpreter getInstance(){instance0}

    @Memoized
    static Apcomplex convert(a){
        convertImpl(a).precision(precision0)
    }

    @Memoized
    private static Apcomplex convertImpl(a){
        if(a==null){
            /* null: let's return 0 instead of null */
            return Apfloat.ZERO
        }else if(a instanceof Apcomplex){
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
             * ii, ij, ji, jj
             * */
            def prefix="^$REGEX_SIGN_OPTIONAL"
            def first="$prefix[ij]\$"
            def secnd="$prefix$REGEX_NUMBER$REGEX_SIGN[ij]$REGEX_NUMBER\$"
            def third="$prefix$REGEX_NUMBER$REGEX_SIGN$REGEX_NUMBER[ij]\$"
            def forth="$prefix[ij]$REGEX_NUMBER\$"
            def fifth="$prefix$REGEX_NUMBER[ij]\$"
            def sixth="^$REGEX_SIGN[ij]{2}\$"
            if(inStr.matches(first)){
                if(inStr.startsWith("-")){
                    return -Apcomplex.I
                }else {
                    return Apcomplex.I
                }
            }else if(inStr.matches(secnd)||inStr.matches(third)){
                def (re,im)=inStr.findAll(Pattern.compile(REGEX_NUMBER)).collect{new Apfloat(it)}
                if(inStr.startsWith("-")){
                    re=-re
                }
                if(inStr.substring(1).contains("-")){
                    im=-im
                }
                return new Apcomplex(re,im)
            }else if(inStr.matches(forth)||inStr.matches(fifth)){
                def im=inStr.findAll(Pattern.compile(REGEX_NUMBER)).collect{new Apfloat(it)}
                def result= new Apcomplex(Apfloat.ZERO,im)
                if(inStr.startsWith("-")){
                    return -result
                }else {
                    return result
                }
            }else if(inStr.matches(sixth)){
                if(inStr.startsWith("-")){
                    return new Apfloat(1)
                }else {
                    return new Apfloat(-1)
                }
            }
        }
        throw new NumberFormatException("We can't understand what you request: $a")
    }

    Impl impl

    private ComplexInterpreter(File cache,ClassLoader loader){
        if(loader instanceof BaseDexClassLoader){
            impl=new AndroidImpl()
        }else{
            impl=new JvmImpl()
        }
        impl.init(cache,loader)
    }

    @CompileStatic
    public Compilation compile(String script){
        return impl.compile(script)
    }

    @CompileStatic
    class Compilation{
        boolean success
        DroidComplexScript script
        Throwable error
    }

    static abstract class DroidComplexScript extends Script{
        @Override
        Object getProperty(String name) {
            return getPropertyImpl(name).with {
                ((it instanceof Apcomplex)?{it.precision(ComplexInterpreter.precision0)}:IDENTITY)(it)
            }
        }

        @Override
        void setProperty(String property, Object newValue) {
            if(property.matches('^[ij]')){
                throw new Error('Cannot set to any field starts with "i" or "j".')
            }
            super.setProperty(property, newValue)
        }

        @Memoized
        Apcomplex i(Object a){(Apcomplex.I*convert(a)).precision(precision0)}
        @Memoized
        Apcomplex j(Object a){i(a)}

        @Memoized
        private Object getPropertyImpl(String name){
            if(name.matches('^[ij]$')){
                return Apcomplex.I
            }else if(name.matches("^[ij]$REGEX_NUMBER\$")||name.matches("^$REGEX_NUMBER[ij]\$")){
                return new Apcomplex(Apfloat.ZERO,new Apfloat(name.findAll(REGEX_NUMBER).first()))
            }else if(name.matches('^[ij]')&&getPropSuper(name.substring(1)) instanceof Number){
                return convert(getPropSuper(name.substring(1)))*Apcomplex.I
            }else if(name.matches('[ij]$')&&getPropSuper(name.substring(0,name.length()-1)) instanceof Number){
                return convert(getPropSuper(name.substring(0,name.length()-1)))*Apcomplex.I
            }
            return getPropSuper(name)
        }

        private Object getPropSuper(String name){super.getProperty(name)}
    }

    private interface Impl{
        void init(File cache,ClassLoader loader)
        Compilation compile(String script)
    }

    @CompileStatic
    private class AndroidImpl implements Impl{
        File cache
        GrooidShell compiler

        @Override
        void init(File cache, ClassLoader loader) {
            this.cache=cache
            this.compiler=new GrooidShell(cache,loader)
        }

        @Override
        Compilation compile(String script) {
            def result=new Compilation()
            try{
                def compileResult=compiler.evaluate(script){CompilerConfiguration cfg->
                    cfg.scriptBaseClass="$DroidComplexScript.name"
                }
                result.success=true
                result.script=(DroidComplexScript)compileResult.script
            }catch (Throwable e){
                result.success=false
                result.error=e
            }
            return result
        }
    }
    @CompileStatic
    private class JvmImpl implements Impl{
        final GroovyShell compiler=new GroovyShell(new CompilerConfiguration().with {
            scriptBaseClass="$DroidComplexScript.name"
            it
        })

        @Override
        void init(File cache, ClassLoader loader) {
        }

        @Override
        Compilation compile(String script) {
            def result=new Compilation()
            try{
                def compileResult=compiler.parse(script)
                result.success=true
                result.script=(DroidComplexScript)compileResult
            }catch (Throwable e){
                result.success=false
                result.error=e
            }
            return result
        }
    }
}
