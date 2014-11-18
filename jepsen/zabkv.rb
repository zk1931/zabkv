role :zabkv do
  task :setup do
    sudo do
      cd '/opt'
      exec! "apt-get -y install maven protobuf-compiler", :echo => true
      exec! "export JAVA_HOME=java-7-openjdk-amd64"
      exec! "rm -rf zabkv", :echo => true
      exec! "rm -rf jzab", :echo => true
      unless dir? 'jzab'
        exec! "git clone https://github.com/EasonLiao/jzab.git", :echo => true
        cd 'jzab'
        exec! "git fetch"
        exec! "git checkout origin/netty -b netty", :echo => true
        exec! "mvn install -DskipTests", :echo => true
      end
      cd '/opt'
      unless dir? 'zabkv'
        exec! "git clone https://github.com/zk1931/zabkv.git", :echo => true
        cd 'zabkv'
        exec! "git fetch"
        exec! "git checkout origin/for_jepsen -b for_jepsen", :echo => true
        exec! "mvn clean compile assembly:single", :echo => true
      end
    end
  end

  task :stop do
    sudo do
      exec! 'ps ax | grep zabkv | grep -v grep | awk \'{ print $1 }\' | xargs kill -s kill'
    end
  end

  task :check do
    sudo do
      cd '/opt/zabkv'
      exec! "md5sum #{name}:5000/transaction.log", :echo => true
    end
  end

  task :check_exception do
    sudo do
      cd '/opt/zabkv'
      exec! "cat log_out | grep Exception", :echo => true
    end
  end

  task :start do
    sudo do
      cd '/opt/zabkv'
      exec! 'rm -rf *5000'
      if name == 'n1'
        exec! "./bin/zabkv 8080 -DserverId=#{name}:5000 >log_out", :echo => true
      else
        # Sleeps for different amount of time to avoid join conflicts.
        sleep name.delete('n').to_i
        exec! "./bin/zabkv 8080 -DserverId=#{name}:5000 -Djoin=n1:5000 >log_out", :echo => true
      end
    end
  end

  task :restart do
    zabkv.stop
    zabkv.start
  end
end
