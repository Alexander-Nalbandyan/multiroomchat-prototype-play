var WebSocketMultiplex = (function(){


    // ****

    var DumbEventTarget = function() {
        this._listeners = {};
    };
    DumbEventTarget.prototype._ensure = function(type) {
        if(!(type in this._listeners)) this._listeners[type] = [];
    };
    DumbEventTarget.prototype.addEventListener = function(type, listener) {
        this._ensure(type);
        this._listeners[type].push(listener);
    };
    DumbEventTarget.prototype.emit = function(type) {
        this._ensure(type);
        var args = Array.prototype.slice.call(arguments, 1);
        if(this['on' + type]) this['on' + type].apply(this, args);
        for(var i=0; i < this._listeners[type].length; i++) {
            this._listeners[type][i].apply(this, args);
        }
    };


    // ****

    var WebSocketMultiplex = function(ws) {
        var that = this;
        this.ws = ws;
        this.channels = {};
        this.ws.addEventListener('message', function(e) {
            var t = JSON.parse(e.data);
            if ('error' in t) {
                // FIXME!!!
                return;
            }

            var type = t.type,
                name = t.topic,
                payload = t.payload;
            if(!(name in that.channels)) {
                return;
            }

            var sub = that.channels[name];
            switch(type) {
            case 'uns':
                delete that.channels[name];
                sub.emit('close', {});
                break;
            case 'msg':
                sub.emit('message', {data: payload});
                break;
            }
        });
    };
    WebSocketMultiplex.prototype.channel = function(raw_name) {
        return this.channels[escape(raw_name)] =
            new Channel(this.ws, escape(raw_name), this.channels);
    };


    var Channel = function(ws, name, channels) {
        DumbEventTarget.call(this);
        var that = this;
        this.ws = ws;
        this.name = name;
        this.channels = channels;
        var onopen = function() {
            var msg = JSON.stringify({
                type: 'sub',
                topic: that.name
            });
            that.ws.send(msg);
            that.emit('open');
        };
        if(ws.readyState > 0) {
            setTimeout(onopen, 0);
        } else {
            this.ws.addEventListener('open', onopen);
        }
    };
    Channel.prototype = new DumbEventTarget();

    Channel.prototype.send = function(data) {
        var msg = JSON.stringify({
            type: 'msg',
            topic: this.name,
            payload: {
                kind: 'talk',
                //user
                //members
                message: data
            }
        });
        this.ws.send(msg);
    };
    Channel.prototype.close = function() {
        var that = this;
        var msg = JSON.stringify({
            type: 'uns',
            topic: this.name
        });
        this.ws.send(msg);
        delete this.channels[this.name];
        setTimeout(function(){that.emit('close', {});},0);
    };

    return WebSocketMultiplex;
})();
