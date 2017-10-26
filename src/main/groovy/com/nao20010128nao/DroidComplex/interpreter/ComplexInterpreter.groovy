package com.nao20010128nao.DroidComplex.interpreter

import android.content.Context
import dalvik.system.DexClassLoader
import groovy.transform.CompileStatic
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

    private static ComplexInterpreter instance
    private static boolean metaClassInstalled=false

    static void install(Context app){
        if(instance){
            return
        }
        instance=new ComplexInterpreter(app?.cacheDir,app?.classLoader)
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

    @CompileStatic
    static ComplexInterpreter getInstance(){instance}

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
            def prefix="^$REGEX_SIGN_OPTIONAL"
            def first="$prefix[ij]\$"
            def secnd="$prefix$REGEX_NUMBER$REGEX_SIGN[ij]$REGEX_NUMBER\$"
            def third="$prefix$REGEX_NUMBER$REGEX_SIGN$REGEX_NUMBER[ij]\$"
            def forth="$prefix[ij]$REGEX_NUMBER\$"
            def fifth="$prefix$REGEX_NUMBER[ij]\$"
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
                if(inStr.substring(1).startsWith("-")){
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
            }
        }
        throw new NumberFormatException("We can't understand what you request: $a")
    }

    Impl impl

    private ComplexInterpreter(File cache,ClassLoader loader){
        if(loader instanceof DexClassLoader){
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

    @CompileStatic
    static abstract class DroidComplexScript extends Script{
        public DroidComplexScript(){
            super(ComplexNumberBinding.instance)
        }

        @Override
        Binding getBinding() {
            return binding=ComplexNumberBinding.instance
        }

        @Override
        void setBinding(Binding binding) {
            super.setBinding(ComplexNumberBinding.instance)
        }
    }

    @Singleton
    static class ComplexNumberBinding extends Binding{
        @Override
        Object getVariable(String name) {
            if(name.matches('^[ij]$')){
                return Apcomplex.I
            }else if(name.matches("^[ij]$REGEX_NUMBER\$")||name.matches("^$REGEX_NUMBER[ij]\$")){
                return new Apcomplex(Apfloat.ZERO,new Apfloat(name.findAll(REGEX_NUMBER).first()))
            }else if(name.matches('^[ij]')&&super.getVariable(name.substring(1)) instanceof Number){
                return super.getVariable(name.substring(1))*Apcomplex.I
            }else if(name.matches('[ij]$')&&super.getVariable(name.substring(0,name.length()-1)) instanceof Number){
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
