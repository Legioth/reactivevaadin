package org.github.legioth.reactivevaadin.internal;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.shared.Registration;

@FunctionalInterface
public interface SubscriptionContext {
    Registration addAttachListener(SerializableFunction<UI, Registration> listener);

    static SubscriptionContext forNode(StateNode node) {
        return attachFunction -> {
            return new Registration() {
    
                private Registration registration;
    
                private final Registration attachRegistration = node.addAttachListener(this::attach);
                private final Registration detachRegistration = node.addDetachListener(this::detach);
    
                {
                    if (node.isAttached()) {
                        attach();
                    }
                }
    
                private void attach() {
                    if (registration == null) {
                        StateTree owner = (StateTree) node.getOwner();
                        registration = attachFunction.apply(owner.getUI());
                    }
                }
    
                private void detach() {
                    if (registration != null) {
                        registration.remove();
                        registration = null;
                    }
                }
    
                @Override
                public void remove() {
                    detach();
                    attachRegistration.remove();
                    detachRegistration.remove();
                }
            };
        };
    }

    static SubscriptionContext forComponent(HasElement component) {
        return SubscriptionContext.forNode(component.getElement().getNode());
    }
}
