package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

public class ElementSelector implements Selector {
	String tag;
	String cls;
	String id;
	class AttrSelector {
		String name;
		String value;
		
		
	}
	
	public ElementSelector(String tag, String cls, String id) {
		super();
		this.tag = tag;
		this.cls = cls;
		this.id = id;
	}

	@Override
	public boolean select(Element node) {
			Element el = (Element) node;
			
			if(tag != null && !el.tagName().equals(tag))
				return false;
			
			if(cls != null && !el.className().equals(cls))
				return false;
			
			if(id != null && !el.id().equals(id))
				return false;
			
			return true;
	}
}
