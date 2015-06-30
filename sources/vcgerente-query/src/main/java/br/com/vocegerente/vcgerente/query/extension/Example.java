package br.com.vocegerente.vcgerente.query.extension;

/**
 * Mantém métodos factory para a construção rápida de Parâmetros para consultas
 * com QBE.
 * 
 * @author augusto
 */
public class Example {

	/**
	 * @return Um parâmetro genérico para utilização em consultas com QBE quando
	 *         se deseja customizar operadores de valores preenchidos no objeto
	 *         exemplo.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static final ParamExample read() {
		return new ParamExample(Object.class);
	}

}
