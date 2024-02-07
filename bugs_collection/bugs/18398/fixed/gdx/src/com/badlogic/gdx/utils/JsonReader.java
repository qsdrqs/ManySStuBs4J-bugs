// line 1 "JsonReader.rl"
// Do not edit this file! Generated by Ragel.
// Ragel.exe -G2 -J -o JsonReader.java JsonReader.rl
/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.badlogic.gdx.files.FileHandle;

/** Lightweight JSON parser.<br>
 * <br>
 * The default behavior is to parse the JSON into a DOM made up of {@link ObjectMap}, {@link Array}, String, Float, and Boolean
 * objects. Extend this class and override methods to perform event driven parsing. When this is done, the parse methods will
 * return null.
 * @author Nathan Sweet */
public class JsonReader {
	public Object parse (String json) {
		char[] data = json.toCharArray();
		return parse(data, 0, data.length);
	}

	public Object parse (Reader reader) {
		try {
			char[] data = new char[1024];
			int offset = 0;
			while (true) {
				int length = reader.read(data, offset, data.length - offset);
				if (length == -1) break;
				if (length == 0) {
					char[] newData = new char[data.length * 2];
					System.arraycopy(data, 0, newData, 0, data.length);
					data = newData;
				} else
					offset += length;
			}
			return parse(data, 0, offset);
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
	}

	public Object parse (InputStream input) {
		try {
			return parse(new InputStreamReader(input, "ISO-8859-1"));
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
	}

	public Object parse (FileHandle file) {
		try {
			return parse(file.read());
		} catch (Exception ex) {
			throw new SerializationException("Error parsing file: " + file, ex);
		}
	}

	public Object parse (char[] data, int offset, int length) {
		int cs, p = offset, pe = length, eof = pe, top = 0;
		int[] stack = new int[4];

		int s = 0;
		Array<String> names = new Array(8);
		boolean needsUnescape = false;
		RuntimeException parseRuntimeEx = null;

		boolean debug = false;
		if (debug) System.out.println();

		try {

			// line 3 "JsonReader.java"
			{
				cs = json_start;
				top = 0;
			}

			// line 8 "JsonReader.java"
			{
				int _klen;
				int _trans = 0;
				int _acts;
				int _nacts;
				int _keys;
				int _goto_targ = 0;

				_goto:
				while (true) {
					switch (_goto_targ) {
					case 0:
						if (p == pe) {
							_goto_targ = 4;
							continue _goto;
						}
						if (cs == 0) {
							_goto_targ = 5;
							continue _goto;
						}
					case 1:
						_match:
						do {
							_keys = _json_key_offsets[cs];
							_trans = _json_index_offsets[cs];
							_klen = _json_single_lengths[cs];
							if (_klen > 0) {
								int _lower = _keys;
								int _mid;
								int _upper = _keys + _klen - 1;
								while (true) {
									if (_upper < _lower) break;

									_mid = _lower + ((_upper - _lower) >> 1);
									if (data[p] < _json_trans_keys[_mid])
										_upper = _mid - 1;
									else if (data[p] > _json_trans_keys[_mid])
										_lower = _mid + 1;
									else {
										_trans += (_mid - _keys);
										break _match;
									}
								}
								_keys += _klen;
								_trans += _klen;
							}

							_klen = _json_range_lengths[cs];
							if (_klen > 0) {
								int _lower = _keys;
								int _mid;
								int _upper = _keys + (_klen << 1) - 2;
								while (true) {
									if (_upper < _lower) break;

									_mid = _lower + (((_upper - _lower) >> 1) & ~1);
									if (data[p] < _json_trans_keys[_mid])
										_upper = _mid - 2;
									else if (data[p] > _json_trans_keys[_mid + 1])
										_lower = _mid + 2;
									else {
										_trans += ((_mid - _keys) >> 1);
										break _match;
									}
								}
								_trans += _klen;
							}
						} while (false);

						cs = _json_trans_targs[_trans];

						if (_json_trans_actions[_trans] != 0) {
							_acts = _json_trans_actions[_trans];
							_nacts = (int)_json_actions[_acts++];
							while (_nacts-- > 0) {
								switch (_json_actions[_acts++]) {
								case 0:
								// line 99 "JsonReader.rl"
								{
									s = p;
									needsUnescape = false;
								}
									break;
								case 1:
								// line 103 "JsonReader.rl"
								{
									needsUnescape = true;
								}
									break;
								case 2:
								// line 106 "JsonReader.rl"
								{
									String name = new String(data, s, p - s);
									s = p;
									if (needsUnescape) name = unescape(name);
									if (debug) System.out.println("name: " + name);
									names.add(name);
								}
									break;
								case 3:
								// line 113 "JsonReader.rl"
								{
									String value = new String(data, s, p - s);
									s = p;
									if (needsUnescape) value = unescape(value);
									String name = names.size > 0 ? names.pop() : null;
									if (debug) System.out.println("string: " + name + "=" + value);
									string(name, value);
								}
									break;
								case 4:
								// line 121 "JsonReader.rl"
								{
									String value = new String(data, s, p - s);
									s = p;
									String name = names.size > 0 ? names.pop() : null;
									if (debug) System.out.println("number: " + name + "=" + Float.parseFloat(value));
									number(name, Float.parseFloat(value));
								}
									break;
								case 5:
								// line 128 "JsonReader.rl"
								{
									String name = names.size > 0 ? names.pop() : null;
									if (debug) System.out.println("boolean: " + name + "=true");
									bool(name, true);
								}
									break;
								case 6:
								// line 133 "JsonReader.rl"
								{
									String name = names.size > 0 ? names.pop() : null;
									if (debug) System.out.println("boolean: " + name + "=false");
									bool(name, false);
								}
									break;
								case 7:
								// line 138 "JsonReader.rl"
								{
									String name = names.size > 0 ? names.pop() : null;
									if (debug) System.out.println("null: " + name);
									string(name, null);
								}
									break;
								case 8:
								// line 143 "JsonReader.rl"
								{
									String name = names.size > 0 ? names.pop() : null;
									if (debug) System.out.println("startObject: " + name);
									startObject(name);
									{
										if (top == stack.length) {
											int[] newStack = new int[stack.length * 2];
											System.arraycopy(stack, 0, newStack, 0, stack.length);
											stack = newStack;
										}
										{
											stack[top++] = cs;
											cs = 9;
											_goto_targ = 2;
											if (true) continue _goto;
										}
									}
								}
									break;
								case 9:
								// line 149 "JsonReader.rl"
								{
									if (debug) System.out.println("endObject");
									pop();
									{
										cs = stack[--top];
										_goto_targ = 2;
										if (true) continue _goto;
									}
								}
									break;
								case 10:
								// line 154 "JsonReader.rl"
								{
									String name = names.size > 0 ? names.pop() : null;
									if (debug) System.out.println("startArray: " + name);
									startArray(name);
									{
										if (top == stack.length) {
											int[] newStack = new int[stack.length * 2];
											System.arraycopy(stack, 0, newStack, 0, stack.length);
											stack = newStack;
										}
										{
											stack[top++] = cs;
											cs = 43;
											_goto_targ = 2;
											if (true) continue _goto;
										}
									}
								}
									break;
								case 11:
								// line 160 "JsonReader.rl"
								{
									if (debug) System.out.println("endArray");
									pop();
									{
										cs = stack[--top];
										_goto_targ = 2;
										if (true) continue _goto;
									}
								}
									break;
								// line 201 "JsonReader.java"
								}
							}
						}

					case 2:
						if (cs == 0) {
							_goto_targ = 5;
							continue _goto;
						}
						if (++p != pe) {
							_goto_targ = 1;
							continue _goto;
						}
					case 4:
						if (p == eof) {
							int __acts = _json_eof_actions[cs];
							int __nacts = (int)_json_actions[__acts++];
							while (__nacts-- > 0) {
								switch (_json_actions[__acts++]) {
								case 3:
								// line 113 "JsonReader.rl"
								{
									String value = new String(data, s, p - s);
									s = p;
									if (needsUnescape) value = unescape(value);
									String name = names.size > 0 ? names.pop() : null;
									if (debug) System.out.println("string: " + name + "=" + value);
									string(name, value);
								}
									break;
								case 4:
								// line 121 "JsonReader.rl"
								{
									String value = new String(data, s, p - s);
									s = p;
									String name = names.size > 0 ? names.pop() : null;
									if (debug) System.out.println("number: " + name + "=" + Float.parseFloat(value));
									number(name, Float.parseFloat(value));
								}
									break;
								case 5:
								// line 128 "JsonReader.rl"
								{
									String name = names.size > 0 ? names.pop() : null;
									if (debug) System.out.println("boolean: " + name + "=true");
									bool(name, true);
								}
									break;
								case 6:
								// line 133 "JsonReader.rl"
								{
									String name = names.size > 0 ? names.pop() : null;
									if (debug) System.out.println("boolean: " + name + "=false");
									bool(name, false);
								}
									break;
								case 7:
								// line 138 "JsonReader.rl"
								{
									String name = names.size > 0 ? names.pop() : null;
									if (debug) System.out.println("null: " + name);
									string(name, null);
								}
									break;
								// line 267 "JsonReader.java"
								}
							}
						}

					case 5:
					}
					break;
				}
			}

			// line 190 "JsonReader.rl"

		} catch (RuntimeException ex) {
			parseRuntimeEx = ex;
		}

		if (p < pe) {
			int lineNumber = 1;
			for (int i = 0; i < p; i++)
				if (data[i] == '\n') lineNumber++;
			throw new SerializationException("Error parsing JSON on line " + lineNumber + " near: " + new String(data, p, pe - p),
				parseRuntimeEx);
		} else if (elements.size != 0) {
			Object element = elements.peek();
			elements.clear();
			if (element instanceof ObjectMap)
				throw new SerializationException("Error parsing JSON, unmatched brace.");
			else
				throw new SerializationException("Error parsing JSON, unmatched bracket.");
		}
		Object root = this.root;
		this.root = null;
		return root;
	}

	// line 277 "JsonReader.java"
	private static byte[] init__json_actions_0 () {
		return new byte[] {0, 1, 0, 1, 1, 1, 2, 1, 3, 1, 4, 1, 5, 1, 6, 1, 7, 1, 8, 1, 9, 1, 10, 1, 11, 2, 0, 2, 2, 0, 3, 2, 3, 9,
			2, 3, 11, 2, 4, 9, 2, 4, 11, 2, 5, 9, 2, 5, 11, 2, 6, 9, 2, 6, 11, 2, 7, 9, 2, 7, 11};
	}

	private static final byte _json_actions[] = init__json_actions_0();

	private static short[] init__json_key_offsets_0 () {
		return new short[] {0, 0, 19, 21, 23, 32, 35, 37, 41, 43, 55, 57, 59, 63, 82, 84, 86, 91, 102, 109, 118, 125, 128, 136,
			138, 147, 151, 153, 160, 170, 178, 186, 194, 202, 207, 215, 223, 231, 236, 244, 252, 260, 265, 274, 295, 297, 299, 304,
			324, 331, 334, 342, 344, 353, 357, 359, 366, 376, 384, 392, 400, 408, 413, 421, 429, 437, 442, 450, 458, 466, 471, 480,
			483, 490, 496, 503, 508, 516, 524, 532, 540, 548, 551, 559, 567, 575, 578, 586, 594, 602, 605, 605};
	}

	private static final short _json_key_offsets[] = init__json_key_offsets_0();

	private static char[] init__json_trans_keys_0 () {
		return new char[] {32, 34, 36, 45, 48, 91, 95, 102, 110, 116, 123, 9, 13, 49, 57, 65, 90, 97, 122, 34, 92, 34, 92, 34, 47,
			92, 98, 102, 110, 114, 116, 117, 48, 49, 57, 48, 57, 43, 45, 48, 57, 48, 57, 32, 34, 36, 44, 95, 125, 9, 13, 65, 90, 97,
			122, 34, 92, 34, 92, 32, 58, 9, 13, 32, 34, 36, 45, 48, 91, 95, 102, 110, 116, 123, 9, 13, 49, 57, 65, 90, 97, 122, 34,
			92, 34, 92, 32, 44, 125, 9, 13, 32, 34, 36, 95, 125, 9, 13, 65, 90, 97, 122, 32, 44, 58, 93, 125, 9, 13, 34, 47, 92, 98,
			102, 110, 114, 116, 117, 32, 44, 58, 93, 125, 9, 13, 48, 49, 57, 32, 44, 46, 69, 101, 125, 9, 13, 48, 57, 32, 44, 69,
			101, 125, 9, 13, 48, 57, 43, 45, 48, 57, 48, 57, 32, 44, 125, 9, 13, 48, 57, 32, 44, 46, 69, 101, 125, 9, 13, 48, 57,
			32, 44, 58, 93, 97, 125, 9, 13, 32, 44, 58, 93, 108, 125, 9, 13, 32, 44, 58, 93, 115, 125, 9, 13, 32, 44, 58, 93, 101,
			125, 9, 13, 32, 44, 125, 9, 13, 32, 44, 58, 93, 117, 125, 9, 13, 32, 44, 58, 93, 108, 125, 9, 13, 32, 44, 58, 93, 108,
			125, 9, 13, 32, 44, 125, 9, 13, 32, 44, 58, 93, 114, 125, 9, 13, 32, 44, 58, 93, 117, 125, 9, 13, 32, 44, 58, 93, 101,
			125, 9, 13, 32, 44, 125, 9, 13, 34, 47, 92, 98, 102, 110, 114, 116, 117, 32, 34, 36, 44, 45, 48, 91, 93, 95, 102, 110,
			116, 123, 9, 13, 49, 57, 65, 90, 97, 122, 34, 92, 34, 92, 32, 44, 93, 9, 13, 32, 34, 36, 45, 48, 91, 93, 95, 102, 110,
			116, 123, 9, 13, 49, 57, 65, 90, 97, 122, 32, 44, 58, 93, 125, 9, 13, 48, 49, 57, 32, 44, 46, 69, 93, 101, 9, 13, 48,
			57, 32, 44, 69, 93, 101, 9, 13, 48, 57, 43, 45, 48, 57, 48, 57, 32, 44, 93, 9, 13, 48, 57, 32, 44, 46, 69, 93, 101, 9,
			13, 48, 57, 32, 44, 58, 93, 97, 125, 9, 13, 32, 44, 58, 93, 108, 125, 9, 13, 32, 44, 58, 93, 115, 125, 9, 13, 32, 44,
			58, 93, 101, 125, 9, 13, 32, 44, 93, 9, 13, 32, 44, 58, 93, 117, 125, 9, 13, 32, 44, 58, 93, 108, 125, 9, 13, 32, 44,
			58, 93, 108, 125, 9, 13, 32, 44, 93, 9, 13, 32, 44, 58, 93, 114, 125, 9, 13, 32, 44, 58, 93, 117, 125, 9, 13, 32, 44,
			58, 93, 101, 125, 9, 13, 32, 44, 93, 9, 13, 34, 47, 92, 98, 102, 110, 114, 116, 117, 32, 9, 13, 32, 44, 58, 93, 125, 9,
			13, 32, 46, 69, 101, 9, 13, 32, 69, 101, 9, 13, 48, 57, 32, 9, 13, 48, 57, 32, 46, 69, 101, 9, 13, 48, 57, 32, 44, 58,
			93, 97, 125, 9, 13, 32, 44, 58, 93, 108, 125, 9, 13, 32, 44, 58, 93, 115, 125, 9, 13, 32, 44, 58, 93, 101, 125, 9, 13,
			32, 9, 13, 32, 44, 58, 93, 117, 125, 9, 13, 32, 44, 58, 93, 108, 125, 9, 13, 32, 44, 58, 93, 108, 125, 9, 13, 32, 9, 13,
			32, 44, 58, 93, 114, 125, 9, 13, 32, 44, 58, 93, 117, 125, 9, 13, 32, 44, 58, 93, 101, 125, 9, 13, 32, 9, 13, 0};
	}

	private static final char _json_trans_keys[] = init__json_trans_keys_0();

	private static byte[] init__json_single_lengths_0 () {
		return new byte[] {0, 11, 2, 2, 7, 1, 0, 2, 0, 6, 2, 2, 2, 11, 2, 2, 3, 5, 5, 7, 5, 1, 6, 0, 5, 2, 0, 3, 6, 6, 6, 6, 6, 3,
			6, 6, 6, 3, 6, 6, 6, 3, 7, 13, 2, 2, 3, 12, 5, 1, 6, 0, 5, 2, 0, 3, 6, 6, 6, 6, 6, 3, 6, 6, 6, 3, 6, 6, 6, 3, 7, 1, 5,
			4, 3, 1, 4, 6, 6, 6, 6, 1, 6, 6, 6, 1, 6, 6, 6, 1, 0, 0};
	}

	private static final byte _json_single_lengths[] = init__json_single_lengths_0();

	private static byte[] init__json_range_lengths_0 () {
		return new byte[] {0, 4, 0, 0, 1, 1, 1, 1, 1, 3, 0, 0, 1, 4, 0, 0, 1, 3, 1, 1, 1, 1, 1, 1, 2, 1, 1, 2, 2, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 4, 0, 0, 1, 4, 1, 1, 1, 1, 2, 1, 1, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
			2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0};
	}

	private static final byte _json_range_lengths[] = init__json_range_lengths_0();

	private static short[] init__json_index_offsets_0 () {
		return new short[] {0, 0, 16, 19, 22, 31, 34, 36, 40, 42, 52, 55, 58, 62, 78, 81, 84, 89, 98, 105, 114, 121, 124, 132, 134,
			142, 146, 148, 154, 163, 171, 179, 187, 195, 200, 208, 216, 224, 229, 237, 245, 253, 258, 267, 285, 288, 291, 296, 313,
			320, 323, 331, 333, 341, 345, 347, 353, 362, 370, 378, 386, 394, 399, 407, 415, 423, 428, 436, 444, 452, 457, 466, 469,
			476, 482, 488, 492, 499, 507, 515, 523, 531, 534, 542, 550, 558, 561, 569, 577, 585, 588, 589};
	}

	private static final short _json_index_offsets[] = init__json_index_offsets_0();

	private static byte[] init__json_trans_targs_0 () {
		return new byte[] {1, 2, 72, 5, 73, 71, 72, 77, 82, 86, 71, 1, 76, 72, 72, 0, 71, 4, 3, 71, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3,
			0, 73, 76, 0, 74, 0, 8, 8, 75, 0, 75, 0, 9, 10, 18, 17, 18, 90, 9, 18, 18, 0, 12, 42, 11, 12, 42, 11, 12, 13, 12, 0, 13,
			14, 20, 21, 22, 16, 20, 29, 34, 38, 16, 13, 28, 20, 20, 0, 16, 19, 15, 16, 19, 15, 16, 17, 90, 16, 0, 17, 10, 18, 18,
			90, 17, 18, 18, 0, 12, 0, 13, 0, 0, 12, 18, 15, 15, 15, 15, 15, 15, 15, 15, 0, 16, 17, 0, 0, 90, 16, 20, 22, 28, 0, 16,
			17, 23, 25, 25, 90, 16, 0, 24, 0, 16, 17, 25, 25, 90, 16, 24, 0, 26, 26, 27, 0, 27, 0, 16, 17, 90, 16, 27, 0, 16, 17,
			23, 25, 25, 90, 16, 28, 0, 16, 17, 0, 0, 30, 90, 16, 20, 16, 17, 0, 0, 31, 90, 16, 20, 16, 17, 0, 0, 32, 90, 16, 20, 16,
			17, 0, 0, 33, 90, 16, 20, 16, 17, 90, 16, 0, 16, 17, 0, 0, 35, 90, 16, 20, 16, 17, 0, 0, 36, 90, 16, 20, 16, 17, 0, 0,
			37, 90, 16, 20, 16, 17, 90, 16, 0, 16, 17, 0, 0, 39, 90, 16, 20, 16, 17, 0, 0, 40, 90, 16, 20, 16, 17, 0, 0, 41, 90, 16,
			20, 16, 17, 90, 16, 0, 11, 11, 11, 11, 11, 11, 11, 11, 0, 43, 44, 48, 47, 49, 50, 46, 91, 48, 57, 62, 66, 46, 43, 56,
			48, 48, 0, 46, 70, 45, 46, 70, 45, 46, 47, 91, 46, 0, 47, 44, 48, 49, 50, 46, 91, 48, 57, 62, 66, 46, 47, 56, 48, 48, 0,
			46, 47, 0, 91, 0, 46, 48, 50, 56, 0, 46, 47, 51, 53, 91, 53, 46, 0, 52, 0, 46, 47, 53, 91, 53, 46, 52, 0, 54, 54, 55, 0,
			55, 0, 46, 47, 91, 46, 55, 0, 46, 47, 51, 53, 91, 53, 46, 56, 0, 46, 47, 0, 91, 58, 0, 46, 48, 46, 47, 0, 91, 59, 0, 46,
			48, 46, 47, 0, 91, 60, 0, 46, 48, 46, 47, 0, 91, 61, 0, 46, 48, 46, 47, 91, 46, 0, 46, 47, 0, 91, 63, 0, 46, 48, 46, 47,
			0, 91, 64, 0, 46, 48, 46, 47, 0, 91, 65, 0, 46, 48, 46, 47, 91, 46, 0, 46, 47, 0, 91, 67, 0, 46, 48, 46, 47, 0, 91, 68,
			0, 46, 48, 46, 47, 0, 91, 69, 0, 46, 48, 46, 47, 91, 46, 0, 45, 45, 45, 45, 45, 45, 45, 45, 0, 71, 71, 0, 71, 0, 0, 0,
			0, 71, 72, 71, 6, 7, 7, 71, 0, 71, 7, 7, 71, 74, 0, 71, 71, 75, 0, 71, 6, 7, 7, 71, 76, 0, 71, 0, 0, 0, 78, 0, 71, 72,
			71, 0, 0, 0, 79, 0, 71, 72, 71, 0, 0, 0, 80, 0, 71, 72, 71, 0, 0, 0, 81, 0, 71, 72, 71, 71, 0, 71, 0, 0, 0, 83, 0, 71,
			72, 71, 0, 0, 0, 84, 0, 71, 72, 71, 0, 0, 0, 85, 0, 71, 72, 71, 71, 0, 71, 0, 0, 0, 87, 0, 71, 72, 71, 0, 0, 0, 88, 0,
			71, 72, 71, 0, 0, 0, 89, 0, 71, 72, 71, 71, 0, 0, 0, 0};
	}

	private static final byte _json_trans_targs[] = init__json_trans_targs_0();

	private static byte[] init__json_trans_actions_0 () {
		return new byte[] {0, 0, 1, 1, 1, 21, 1, 1, 1, 1, 17, 0, 1, 1, 1, 0, 28, 1, 1, 7, 0, 0, 3, 3, 3, 3, 3, 3, 3, 3, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 19, 0, 1, 1, 0, 25, 1, 1, 5, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 21, 1, 1, 1, 1, 17,
			0, 1, 1, 1, 0, 28, 1, 1, 7, 0, 0, 0, 0, 19, 0, 0, 0, 0, 1, 1, 19, 0, 1, 1, 0, 5, 0, 5, 0, 0, 5, 0, 3, 3, 3, 3, 3, 3, 3,
			3, 0, 7, 7, 0, 0, 31, 7, 0, 0, 0, 0, 9, 9, 0, 0, 0, 37, 9, 0, 0, 0, 9, 9, 0, 0, 37, 9, 0, 0, 0, 0, 0, 0, 0, 0, 9, 9, 37,
			9, 0, 0, 9, 9, 0, 0, 0, 37, 9, 0, 0, 7, 7, 0, 0, 0, 31, 7, 0, 7, 7, 0, 0, 0, 31, 7, 0, 7, 7, 0, 0, 0, 31, 7, 0, 7, 7, 0,
			0, 0, 31, 7, 0, 13, 13, 49, 13, 0, 7, 7, 0, 0, 0, 31, 7, 0, 7, 7, 0, 0, 0, 31, 7, 0, 7, 7, 0, 0, 0, 31, 7, 0, 15, 15,
			55, 15, 0, 7, 7, 0, 0, 0, 31, 7, 0, 7, 7, 0, 0, 0, 31, 7, 0, 7, 7, 0, 0, 0, 31, 7, 0, 11, 11, 43, 11, 0, 3, 3, 3, 3, 3,
			3, 3, 3, 0, 0, 0, 1, 0, 1, 1, 21, 23, 1, 1, 1, 1, 17, 0, 1, 1, 1, 0, 28, 1, 1, 7, 0, 0, 0, 0, 23, 0, 0, 0, 0, 1, 1, 1,
			21, 23, 1, 1, 1, 1, 17, 0, 1, 1, 1, 0, 7, 7, 0, 34, 0, 7, 0, 0, 0, 0, 9, 9, 0, 0, 40, 0, 9, 0, 0, 0, 9, 9, 0, 40, 0, 9,
			0, 0, 0, 0, 0, 0, 0, 0, 9, 9, 40, 9, 0, 0, 9, 9, 0, 0, 40, 0, 9, 0, 0, 7, 7, 0, 34, 0, 0, 7, 0, 7, 7, 0, 34, 0, 0, 7, 0,
			7, 7, 0, 34, 0, 0, 7, 0, 7, 7, 0, 34, 0, 0, 7, 0, 13, 13, 52, 13, 0, 7, 7, 0, 34, 0, 0, 7, 0, 7, 7, 0, 34, 0, 0, 7, 0,
			7, 7, 0, 34, 0, 0, 7, 0, 15, 15, 58, 15, 0, 7, 7, 0, 34, 0, 0, 7, 0, 7, 7, 0, 34, 0, 0, 7, 0, 7, 7, 0, 34, 0, 0, 7, 0,
			11, 11, 46, 11, 0, 3, 3, 3, 3, 3, 3, 3, 3, 0, 0, 0, 0, 7, 0, 0, 0, 0, 7, 0, 9, 0, 0, 0, 9, 0, 9, 0, 0, 9, 0, 0, 9, 9, 0,
			0, 9, 0, 0, 0, 9, 0, 0, 7, 0, 0, 0, 0, 0, 7, 0, 7, 0, 0, 0, 0, 0, 7, 0, 7, 0, 0, 0, 0, 0, 7, 0, 7, 0, 0, 0, 0, 0, 7, 0,
			13, 13, 0, 7, 0, 0, 0, 0, 0, 7, 0, 7, 0, 0, 0, 0, 0, 7, 0, 7, 0, 0, 0, 0, 0, 7, 0, 15, 15, 0, 7, 0, 0, 0, 0, 0, 7, 0, 7,
			0, 0, 0, 0, 0, 7, 0, 7, 0, 0, 0, 0, 0, 7, 0, 11, 11, 0, 0, 0, 0};
	}

	private static final byte _json_trans_actions[] = init__json_trans_actions_0();

	private static byte[] init__json_eof_actions_0 () {
		return new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 9, 9,
			9, 9, 7, 7, 7, 7, 13, 7, 7, 7, 15, 7, 7, 7, 11, 0, 0};
	}

	private static final byte _json_eof_actions[] = init__json_eof_actions_0();

	static final int json_start = 1;
	static final int json_first_final = 71;
	static final int json_error = 0;

	static final int json_en_object = 9;
	static final int json_en_array = 43;
	static final int json_en_main = 1;

	// line 214 "JsonReader.rl"

	private final Array elements = new Array(8);
	private Object root, current;

	private void set (String name, Object value) {
		if (current instanceof ObjectMap)
			((ObjectMap)current).put(name, value);
		else if (current instanceof Array)
			((Array)current).add(value);
		else
			root = value;
	}

	protected void startObject (String name) {
		ObjectMap value = new ObjectMap();
		if (current != null) set(name, value);
		elements.add(value);
		current = value;
	}

	protected void startArray (String name) {
		Array value = new Array();
		if (current != null) set(name, value);
		elements.add(value);
		current = value;
	}

	protected void pop () {
		root = elements.pop();
		current = elements.size > 0 ? elements.peek() : null;
	}

	protected void string (String name, String value) {
		set(name, value);
	}

	protected void number (String name, float value) {
		set(name, value);
	}

	protected void bool (String name, boolean value) {
		set(name, value);
	}

	private String unescape (String value) {
		int length = value.length();
		StringBuilder buffer = new StringBuilder(length + 16);
		for (int i = 0; i < length;) {
			char c = value.charAt(i++);
			if (c != '\\') {
				buffer.append(c);
				continue;
			}
			if (i == length) break;
			c = value.charAt(i++);
			if (c == 'u') {
				buffer.append(Character.toChars(Integer.parseInt(value.substring(i, i + 4), 16)));
				i += 4;
				continue;
			}
			switch (c) {
			case '"':
			case '\\':
			case '/':
				break;
			case 'b':
				c = '\b';
				break;
			case 'f':
				c = '\f';
				break;
			case 'n':
				c = '\n';
				break;
			case 'r':
				c = '\r';
				break;
			case 't':
				c = '\t';
				break;
			default:
				throw new SerializationException("Illegal escaped character: \\" + c);
			}
			buffer.append(c);
		}
		return buffer.toString();
	}
}
